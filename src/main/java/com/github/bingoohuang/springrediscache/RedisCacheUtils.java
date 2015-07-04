package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.codec.Json;
import com.github.bingoohuang.utils.redis.Redis;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Files;
import com.google.common.primitives.Primitives;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static com.github.bingoohuang.springrediscache.RedisFor.StoreValue;
import static org.springframework.util.StringUtils.capitalize;

public class RedisCacheUtils {
    static LoadingCache<Class<?>, Optional<Method>> redisCacheExpirationAwareTagMethodCache
            = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, Optional<Method>>() {
        @Override
        public Optional<Method> load(Class<?> aClass) throws Exception {
            return searchRedisCacheExpirationAwareTagMethod(aClass);
        }
    });

    static Method findRedisCacheExpirationAwareTagMethod(Class<?> aClass) {
        return redisCacheExpirationAwareTagMethodCache.getUnchecked(aClass).orNull();
    }

    private static Optional<Method> searchRedisCacheExpirationAwareTagMethod(Class<?> aClass) {
        Logger log = LoggerFactory.getLogger(aClass);

        for (Method method : aClass.getMethods()) {
            RedisCacheExpirationAwareTag awareTag = method.getAnnotation(RedisCacheExpirationAwareTag.class);
            if (awareTag == null) continue;
            if (method.getParameterTypes().length != 0) {
                log.warn("@RedisCacheExpirationAwareTag method should be non arguments");
                continue;
            }

            Class<?> returnType = method.getReturnType();
            if (returnType.isPrimitive()) returnType = Primitives.wrap(returnType);
            if (!Number.class.isAssignableFrom(returnType)) {
                log.warn("@RedisCacheExpirationAwareTag method should be return Number type");
                continue;
            }

            return Optional.of(method);
        }

        return Optional.absent();
    }


    public static long redisExpirationSeconds(String key, ApplicationContext appContext) {
        Redis redis = tryGetBean(appContext, Redis.class);

        String expirationStr = redis != null ? redis.get(key) : cwdFileRefreshSeconds(key);
        long expirationSeconds = Consts.MaxSeconds;
        if (expirationStr == null) return expirationSeconds;
        if (expirationStr.matches("\\d+")) return Consts.MinSeconds + Long.parseLong(expirationStr);
        return Consts.MinSeconds + expirationStr.hashCode();
    }

    private static String cwdFileRefreshSeconds(String key) {
        File file = new File(key.replace(':', '.'));
        if (!file.exists() || !file.isFile()) return null;

        try {
            return Files.toString(file, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("try to read cwdFileRefreshSeconds for key " + key + " failed", e);
        }
    }

    public static <T> T createObject(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T getBean(ApplicationContext appContext, Class<T> beanClass) {
        try {
            return appContext.getBean(beanClass);
        } catch (BeansException e) {
            return RedisCacheUtils.createObject(beanClass);
        }
    }

    static Object invokeMethod(MethodInvocation invocation, ApplicationContext appContext) {
        Method method = invocation.getMethod();
        Class<?> declaringClass = method.getDeclaringClass();
        RedisCacheTargetMock redisCacheTargetMock = declaringClass.getAnnotation(RedisCacheTargetMock.class);
        if (redisCacheTargetMock == null) return invokeMethod(invocation);

        String className = redisCacheTargetMock.value();
        try {
            Class clazz = Class.forName(className);
            Object bean = appContext.getBean(clazz);
            Method mockMethod = clazz.getMethod(method.getName(), method.getParameterTypes());
            return mockMethod.invoke(bean, invocation.getArguments());
        } catch (Exception e) {
            return invokeMethod(invocation);
        }
    }

    private static Object invokeMethod(MethodInvocation invocation) {
        try {
            Object threadLocalValue = RedisCacheConnecter.threadLocal.get();
            if (threadLocalValue != null) return threadLocalValue;

            return invocation.proceed();
        } catch (Throwable throwable) {
            throw Throwables.propagate(throwable);
        }
    }

    public static String joinArguments(MethodInvocation invocation) {
        StringBuilder sb = new StringBuilder();

        for (Object arg : invocation.getArguments()) {
            sb.append(convert(arg)).append(':');
        }

        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        else sb.append("NoArgs");

        return sb.toString();
    }

    private static String convert(Object arg) {
        if (arg == null) return null;

        if (arg.getClass().isPrimitive()) return arg.toString();
        if (Primitives.isWrapperType(arg.getClass())) return arg.toString();
        if (arg instanceof Enum) return arg.toString();
        if (arg instanceof CharSequence) return arg.toString();

        return Json.json(arg);
    }

    public static void sleep(int milis) {
        try {
            TimeUnit.MILLISECONDS.sleep(milis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    static ValueSerializable createValueSerializer(ApplicationContext appContext,
                                                   RedisCacheEnabled redisCacheAnn,
                                                   Method method, Logger logger) {
        if (redisCacheAnn.redisFor() != StoreValue) return null;

        Class<?> returnType = method.getReturnType();
        if (redisCacheAnn.valueSerializer() == AutoSelectValueSerializer.class) {
            if (returnType == String.class) return new StringValueSerializable();
            if (returnType.isPrimitive() || Primitives.isWrapperType(returnType))
                return new PrimitiveValueSerializable(returnType);
            if (returnType.isEnum()) return new EnumValueSerializable(returnType);

            return new JSONValueSerializer(returnType, logger);
        }

        try {
            return appContext.getBean(redisCacheAnn.valueSerializer());
        } catch (BeansException e) {
            logger.warn("unable to get spring bean by " + redisCacheAnn.valueSerializer());
        }

        ValueSerializable serializable = RedisCacheUtils.createObject(redisCacheAnn.valueSerializer());
        if (serializable == null) {
            logger.warn("unable to create instance for " + redisCacheAnn.valueSerializer()
                    + ", use " + JSONValueSerializer.class + " for instead");
            serializable = new JSONValueSerializer(returnType, logger);
        }

        return serializable;

    }

    static String generateValueKey(MethodInvocation invocation,
                                   RedisCacheEnabled redisCacheAnn,
                                   ApplicationContext appContext,
                                   Logger logger) {
        Class<? extends RedisCacheNameGenerator> naming = redisCacheAnn.naming();
        if (naming != NoopRedisCacheNameGenerator.class) {
            RedisCacheNameGenerator bean = getBean(appContext, naming);
            if (bean != null) return bean.generateCacheName(invocation);

            logger.warn("fail to get/create instance for {}", naming);
        }

        String arguments = joinArguments(invocation);
        return "Cache:" + getNamePrefix(invocation) + ":" + arguments;
    }

    private static String getNamePrefix(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        String methodName = method.getName();
        String simpleName = method.getDeclaringClass().getSimpleName() + ":";
        if (methodName.startsWith("get")) methodName = methodName.substring(3);
        return simpleName + capitalize(methodName);
    }

    public static <T> T tryGetBean(ApplicationContext appContext, Class<T> clazz) {
        try {
            return appContext.getBean(clazz);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }
}
