package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.codec.Json;
import com.github.bingoohuang.utils.redis.Redis;
import net.jodah.expiringmap.ExpiringMap;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.jodah.expiringmap.ExpiringMap.ExpirationPolicy.CREATED;

class InvocationRuntime {
    private final MethodInvocation invocation;
    private final RedisCacheEnabled redisCacheAnn;
    private final String valueKey;
    private final String lockKey;
    private final Redis redis;
    private final ExpiringMap<String, CachedValueWrapper> localCache;
    private final ApplicationContext appContext;
    private final ScheduledExecutorService executorService;
    private final Logger logger;
    private Object value;

    public InvocationRuntime(MethodInvocation invocation, Redis redis, ExpiringMap<String, CachedValueWrapper> localCache,
                             ApplicationContext appContext, ScheduledExecutorService executorService) {
        this.logger = LoggerFactory.getLogger(invocation.getMethod().getDeclaringClass());
        this.invocation = invocation;
        this.redisCacheAnn = invocation.getMethod().getAnnotation(RedisCacheEnabled.class);
        this.redis = redis;
        this.localCache = localCache;
        this.appContext = appContext;
        this.executorService = executorService;

        this.valueKey = generateValueKey(invocation);
        this.lockKey = valueKey + ":lock";
    }

    private String generateValueKey(MethodInvocation invocation) {
        Class<? extends CacheNameGenerator> naming = redisCacheAnn.naming();
        if (naming != NOOP.class) {
            CacheNameGenerator bean;
            try {
                bean = appContext.getBean(naming);
            } catch (BeansException e) {
                bean = createObject(naming);
            }

            if (bean == null) {
                logger.warn("fail to get/create instance for {}", naming);
            } else {
                return bean.generateCacheName(invocation);
            }
        }

        String firstPrefix = getFirstPrefix();
        String prefix = getNamePrefix();
        String arguments = Args.joinArguments(invocation);
        return firstPrefix + prefix + ":" + arguments;
    }

    public static <T> T createObject(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            return null;
        }
    }


    private String getNamePrefix() {
        Method method = invocation.getMethod();
        String methodName = method.getName();
        String simpleName = method.getDeclaringClass().getSimpleName() + ":";
        if (methodName.startsWith("get")) methodName = methodName.substring(3);
        return simpleName + methodName;
    }

    private String getFirstPrefix() {
        String prefix = redisCacheAnn.prefix();
        if (StringUtils.isEmpty(prefix)) prefix = "Cache:";
        return prefix;
    }

    public long aheadMillis() {
        return redisCacheAnn.aheadMillis();
    }

    public long expirationMillis() {
        return redisCacheAnn.expirationMillis();
    }

    public void invokeMethod() {
        this.value = Invokes.invokeMethod(invocation);
    }

    public boolean tryRedisLock() {
        boolean locked = redis.tryLock(lockKey);
        if (locked) logger.debug("got  redis lock {}", lockKey);
        return locked;
    }

    public void unlockRedis(boolean locked) {
        if (locked) {
            redis.del(lockKey);
            logger.debug("free redis lock {}", lockKey);
        }
    }

    public void setex(long millis) {
        String value = Json.jsonWithType(this.value);
        redis.setex(valueKey, value, millis, TimeUnit.MILLISECONDS);
        logger.debug("put  redis {} = {} with expiration {} millis", valueKey, value, millis);
    }

    public Object getLocalCache() {
        CachedValueWrapper cachedValue = localCache.get(valueKey);
        if (cachedValue != null) {
            long expectedExpiration = getExpectedExpiration();
            logger.debug("got  local {} = {} with expiration {} millis", valueKey, cachedValue.getValue(), expectedExpiration);
            if (expectedExpiration > 0) this.value = cachedValue.getValue();
            else localCache.remove(valueKey);
        } else {
            logger.debug("got  local {} = null", valueKey);
        }

        return this.value;
    }

    private long getExpectedExpiration() {
        try {
            return localCache.getExpectedExpiration(valueKey);
        } catch (NoSuchElementException e) {
            return -1;
        }
    }

    public long redisTtl() {
        return redis.ttl(valueKey);
    }

    public void putCache(long millis) {
        CachedValueWrapper valueWrapper = new CachedValueWrapper(this.value, redisCacheAnn, logger);
        localCache.put(valueKey, valueWrapper, CREATED, millis, TimeUnit.MILLISECONDS);
        logger.debug("put  local {} = {} with expiration {} millis", valueKey, this.value, millis);
    }

    public void waitLockReleaseAndReadRedis() {
        waitRedisLock();
        loadRedisValueToCache(-1);
    }

    public Object getValue() {
        return value;
    }

    public void loadRedisValueToCache(long ttl) {
        String redisValue = redis.get(valueKey);
        logger.debug("got  redis {} = {}", valueKey, redisValue);
        value = Json.unJsonWithType(redisValue);

        putCache(ttl > 0 ? ttl : redisTtl());
    }

    public boolean isBeforeAheadMillis() {
        return getExpectedExpiration() > aheadMillis();
    }

    public Object process() {
        CacheProcessor cacheProcessor = redisCacheAnn.redisStore()
                ? new StoreProcessor(this)
                : new NotifyProcessor(this);
        return cacheProcessor.process();
    }

    public void submit(Runnable runnable) {
        executorService.submit(runnable);
    }

    public boolean tryLockLocalCache() {
        Object prev = localCache.putIfAbsent(lockKey, CachedValueWrapper.instance);
        return prev == null;
    }

    public void unlockLocalCache() {
        localCache.remove(lockKey);
        logger.debug("free local lock {}", lockKey);
    }

    public Object invokeMethodAndPutCache() {
        invokeMethod();

        long expiration = Utils.redisExpiration(valueKey, redis);
        putCache(expiration);

        return value;
    }

    public void waitCacheLock() {
        logger.debug("wait cache lock {}", lockKey);
        while (!tryLockLocalCache()) {
            Threads.sleep(100);
        }
        logger.debug("got  cache lock {}", lockKey);
    }

    private void waitRedisLock() {
        logger.debug("wait redis lock {}", lockKey);
        do {
            Threads.sleep(100);
        } while (redis.isLocked(lockKey));
        logger.debug("got  redis lock {}", lockKey);
    }
}
