package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.codec.Json;
import com.github.bingoohuang.utils.redis.Redis;
import com.google.common.primitives.Primitives;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.TimeUnit;

class Utils {
    public static long redisExpiration(String key, Redis redis) {
        String expirationStr = redis.get(key);
        long expiration = Consts.MaxMillis;
        if (expirationStr == null) return expiration;
        if (expirationStr.matches("\\d+")) return Consts.MinMillis + Long.parseLong(expirationStr);
        return Consts.MinMillis + expirationStr.hashCode();
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
            return Utils.createObject(beanClass);
        }
    }

    static Object invokeMethod(MethodInvocation invocation) {
        try {
            return invocation.proceed();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static String joinArguments(MethodInvocation invocation) {
        StringBuilder sb = new StringBuilder();

        for (Object arg : invocation.getArguments()) {
            sb.append(convert(arg)).append(':');
        }

        if (sb.length() > 0) sb.setLength(sb.length() - 1);
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
}
