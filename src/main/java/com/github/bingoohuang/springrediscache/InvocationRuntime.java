package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.redis.Redis;
import com.google.common.base.Optional;
import net.jodah.expiringmap.ExpiringMap;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.bingoohuang.springrediscache.RedisCacheConnector.CLEARTAG;
import static com.github.bingoohuang.springrediscache.RedisCacheConnector.THREADLOCAL;
import static com.github.bingoohuang.springrediscache.RedisCacheUtils.*;
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
    final Logger logger;
    private final ValueSerializable valueSerializer;
    private Object value;

    InvocationRuntime(MethodInvocation invocation, ExpiringMap<String, CachedValueWrapper> localCache,
                      ApplicationContext appContext, ScheduledExecutorService executorService) {
        this.logger = LoggerFactory.getLogger(invocation.getMethod().getDeclaringClass());
        this.invocation = invocation;
        this.redisCacheAnn = invocation.getMethod().getAnnotation(RedisCacheEnabled.class);
        this.redis = RedisCacheUtils.tryGetBean(appContext, Redis.class);
        this.localCache = localCache;
        this.appContext = appContext;
        this.executorService = executorService;

        this.valueKey = generateValueKey(invocation, redisCacheAnn, appContext, logger);
        this.valueSerializer = createValueSerializer(appContext, redisCacheAnn, invocation.getMethod(), logger);
        this.lockKey = valueKey + ":lock";
    }

    long expirationSeconds() {
        return redisCacheAnn.expirationSeconds();
    }

    Object invokeMethod() {
        this.value = RedisCacheUtils.invokeMethod(invocation, appContext);
        return this.value;
    }

    boolean tryRedisLock() {
        boolean locked = redis.tryLock(lockKey);
        if (locked) logger.debug("got redis lock {}", lockKey);
        return locked;
    }

    void unlockRedis(boolean locked) {
        if (locked) {
            redis.del(lockKey);
            logger.debug("free redis lock {}", lockKey);
        }
    }

    void setex(long expirationSeconds) {
        logger.debug("put redis {} = {} with expiration {} seconds",
                valueKey, value, expirationSeconds);

        String value = valueSerializer.serialize(this.value);
        redis.setex(valueKey, value, expirationSeconds, TimeUnit.SECONDS);
    }

    Object getLocalCache() {
        CachedValueWrapper cachedValue = localCache.get(valueKey);
        if (cachedValue != null) {
            long expectedExpiration = getExpectedExpirationSeconds();
            logger.debug("got local {} = {} with expiration {} seconds",
                    valueKey, cachedValue.getValue(), expectedExpiration);
            if (expectedExpiration >= 0) this.value = cachedValue.getValue();
            else localCache.remove(valueKey);
        } else {
            logger.debug("got local {} = null", valueKey);
        }

        return this.value;
    }

    private long getExpectedExpirationSeconds() {
        try {
            return localCache.getExpectedExpiration(valueKey) / 1000;
        } catch (NoSuchElementException e) {
            return -1;
        }
    }

    long redisTtlSeconds() {
        return redis.ttl(valueKey);
    }

    void putLocalCache(long ttlSeconds) {
        logger.debug("put local {} = {} with expiration {} seconds",
                valueKey, this.value, ttlSeconds);
        CachedValueWrapper vw = new CachedValueWrapper(this.value, redisCacheAnn, logger);
        localCache.put(valueKey, vw, CREATED, ttlSeconds, TimeUnit.SECONDS);
    }

    void waitLockReleaseAndReadRedis() {
        waitRedisLock();
        loadRedisValueToCache(-1);
    }

    Object getValue() {
        return value;
    }

    boolean loadRedisValueToCache(long ttlSeconds) {
        String redisValue = redis.get(valueKey);
        logger.debug("got redis {} = {}", valueKey, redisValue);

        if (StringUtils.isEmpty(redisValue)) {
            value = null;
            return false;
        } else {
            value = valueSerializer.deserialize(redisValue, invocation.getMethod());
            if (value != null)
                putLocalCache(ttlSeconds > 0 ? ttlSeconds : redisTtlSeconds());
            return true;
        }
    }

    boolean isBeforeAheadSeconds() {
        return getExpectedExpirationSeconds() > Consts.AheadRefreshSeconds;
    }

    boolean isAheadRefreshEnabled() {
        return redisCacheAnn.aheadRefresh() &&
                redisCacheAnn.expirationSeconds() > Consts.AheadRefreshSeconds;
    }

    Object process() {
        checkRedisRequired();
        tryInvalidCacheIfConnectingCache();

        CacheProcessor cacheProcessor = getCacheProcessor();
        return cacheProcessor.process();
    }

    private CacheProcessor getCacheProcessor() {
        switch (redisCacheAnn.redisFor()) {
            case StoreValue:
                return new StoreValueProcessor(this);
            case RefreshSeconds:
            case CwdFileRefreshSeconds:
            default:
                return new RefreshSecondsProcessor(this);
        }
    }

    private void checkRedisRequired() {
        switch (redisCacheAnn.redisFor()) {
            case StoreValue:
            case RefreshSeconds:
                if (redis != null) return;
                throw new RuntimeException("Redis bean should defined in spring context");
        }
    }

    private void tryInvalidCacheIfConnectingCache() {
        Optional<Object> threadLocalValue = THREADLOCAL.get();
        if (threadLocalValue == null) return;

        CachedValueWrapper removed = localCache.remove(valueKey);

        if (redisCacheAnn.redisFor() == RedisFor.StoreValue) {
            if (threadLocalValue.orNull() == CLEARTAG) {
                String redisValue = redis.get(valueKey);
                String serialize = removed != null && redisValue != null
                        ? valueSerializer.serialize(removed.getValue())
                        : null;
                if (redisValue != null && redisValue.equals(serialize)) {
                    redis.del(valueKey);
                }
            } else {
                redis.del(valueKey);
            }
        }
    }

    void submit(Runnable runnable) {
        executorService.submit(runnable);
    }

    boolean tryLockLocalCache() {
        Object prev = localCache.putIfAbsent(lockKey, CachedValueWrapper.instance);
        return prev == null;
    }

    void unlockLocalCache() {
        localCache.remove(lockKey);
        logger.debug("free local lock {}", lockKey);
    }

    Object invokeMethodAndPutCache() {
        invokeMethod();
        if (value != null) {
            long expirationSeconds = trySaveExpireSeconds(
                    redisCacheAnn.redisFor(), valueKey, appContext, this);
            putLocalCache(expirationSeconds);
        }

        return value;
    }

    void waitCacheLock() {
        logger.debug("wait cache lock {}", lockKey);
        while (!tryLockLocalCache()) {
            RedisCacheUtils.sleep(100);
        }
        logger.debug("got cache lock {}", lockKey);
    }

    private void waitRedisLock() {
        logger.debug("wait redis lock {}", lockKey);
        do {
            RedisCacheUtils.sleep(100);
        } while (redis.isLocked(lockKey));
        logger.debug("got redis lock {}", lockKey);
    }

}
