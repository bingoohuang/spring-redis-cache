package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.redis.Redis;
import net.jodah.expiringmap.ExpiringMap;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

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

        this.valueKey = RedisCacheUtils.generateValueKey(invocation, redisCacheAnn, appContext, logger);
        this.valueSerializer = RedisCacheUtils.createValueSerializer(appContext, redisCacheAnn, invocation.getMethod(), logger);
        this.lockKey = valueKey + ":lock";
    }

    long expirationSeconds() {
        return redisCacheAnn.expirationSeconds();
    }

    void invokeMethod() {
        this.value = RedisCacheUtils.invokeMethod(invocation, appContext);
    }

    boolean tryRedisLock() {
        boolean locked = redis.tryLock(lockKey);
        if (locked) logger.debug("got  redis lock {}", lockKey);
        return locked;
    }

    void unlockRedis(boolean locked) {
        if (locked) {
            redis.del(lockKey);
            logger.debug("free redis lock {}", lockKey);
        }
    }

    void setex(long expirationSeconds) {
        String value = valueSerializer.serialize(this.value);
        redis.setex(valueKey, value, expirationSeconds, TimeUnit.SECONDS);
        logger.debug("put  redis {} = {} with expiration {} seconds", valueKey, value, expirationSeconds);
    }

    Object getLocalCache() {
        CachedValueWrapper cachedValue = localCache.get(valueKey);
        if (cachedValue != null) {
            long expectedExpiration = getExpectedExpirationSeconds();
            logger.debug("got  local {} = {} with expiration {} seconds", valueKey, cachedValue.getValue(), expectedExpiration);
            if (expectedExpiration >= 0) this.value = cachedValue.getValue();
            else localCache.remove(valueKey);
        } else {
            logger.debug("got  local {} = null", valueKey);
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
        CachedValueWrapper valueWrapper = new CachedValueWrapper(this.value, redisCacheAnn, logger);
        localCache.put(valueKey, valueWrapper, CREATED, ttlSeconds, TimeUnit.SECONDS);
        logger.debug("put  local {} = {} with expiration {} seconds", valueKey, this.value, ttlSeconds);
    }

    void waitLockReleaseAndReadRedis() {
        waitRedisLock();
        loadRedisValueToCache(-1);
    }

    Object getValue() {
        return value;
    }

    void loadRedisValueToCache(long ttlSeconds) {
        String redisValue = redis.get(valueKey);
        logger.debug("got  redis {} = {}", valueKey, redisValue);

        if (StringUtils.isEmpty(redisValue)) value = null;
        else {
            value = valueSerializer.deserialize(redisValue, invocation.getMethod());
            if (value != null) putLocalCache(ttlSeconds > 0 ? ttlSeconds : redisTtlSeconds());
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
        switch (redisCacheAnn.redisFor()) {
            case StoreValue:
                checkRedisRequired();
                return new StoreValueProcessor(this).process();
            case RefreshSeconds:
                checkRedisRequired();
            case CwdFileRefreshSeconds:
                return new RefreshSecondsProcessor(this).process();
        }

        throw new RuntimeException("code should be reached here");
    }

    private void checkRedisRequired() {
        if (redis != null) return;

        throw new RuntimeException("Redis bean should defined in spring context");
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

        long expiration = RedisCacheUtils.redisExpirationSeconds(valueKey, appContext);
        putLocalCache(expiration);

        return value;
    }

    void waitCacheLock() {
        logger.debug("wait cache lock {}", lockKey);
        while (!tryLockLocalCache()) {
            RedisCacheUtils.sleep(100);
        }
        logger.debug("got  cache lock {}", lockKey);
    }

    private void waitRedisLock() {
        logger.debug("wait redis lock {}", lockKey);
        do {
            RedisCacheUtils.sleep(100);
        } while (redis.isLocked(lockKey));
        logger.debug("got  redis lock {}", lockKey);
    }
}
