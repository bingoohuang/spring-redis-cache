package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.codec.Json;
import com.github.bingoohuang.utils.redis.Redis;
import net.jodah.expiringmap.ExpiringMap;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.bingoohuang.springrediscache.RedisFor.StoreValue;
import static net.jodah.expiringmap.ExpiringMap.ExpirationPolicy.CREATED;
import static org.springframework.util.StringUtils.capitalize;

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

    InvocationRuntime(MethodInvocation invocation, Redis redis, ExpiringMap<String, CachedValueWrapper> localCache,
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
        Class<? extends RedisCacheNameGenerator> naming = redisCacheAnn.naming();
        if (naming != NoopRedisCacheNameGenerator.class) {
            RedisCacheNameGenerator bean = Utils.getBean(appContext, naming);
            if (bean != null) return bean.generateCacheName(invocation);

            logger.warn("fail to get/create instance for {}", naming);
        }

        String arguments = Utils.joinArguments(invocation);
        return "Cache:" + getNamePrefix() + ":" + arguments;
    }

    private String getNamePrefix() {
        Method method = invocation.getMethod();
        String methodName = method.getName();
        String simpleName = method.getDeclaringClass().getSimpleName() + ":";
        if (methodName.startsWith("get")) methodName = methodName.substring(3);
        return simpleName + capitalize(methodName);
    }

    long expirationSeconds() {
        return redisCacheAnn.expirationSeconds();
    }

    void invokeMethod() {
        this.value = Utils.invokeMethod(invocation, appContext);
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
        String value = Json.jsonWithType(this.value);
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
        value = Json.unJsonWithType(redisValue);

        putLocalCache(ttlSeconds > 0 ? ttlSeconds : redisTtlSeconds());
    }

    boolean isBeforeAheadSeconds() {
        return getExpectedExpirationSeconds() > Consts.AheadRefreshSeconds;
    }

    boolean isAheadRefreshEnabled() {
        return redisCacheAnn.aheadRefresh() &&
                redisCacheAnn.expirationSeconds() > Consts.AheadRefreshSeconds;
    }

    Object process() {
        CacheProcessor cacheProcessor =
                redisCacheAnn.redisFor() == StoreValue
                        ? new StoreValueProcessor(this)
                        : new RefreshMillisProcessor(this);
        return cacheProcessor.process();
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

        long expiration = Utils.redisExpirationSeconds(valueKey, redis);
        putLocalCache(expiration);

        return value;
    }

    void waitCacheLock() {
        logger.debug("wait cache lock {}", lockKey);
        while (!tryLockLocalCache()) {
            Utils.sleep(100);
        }
        logger.debug("got  cache lock {}", lockKey);
    }

    private void waitRedisLock() {
        logger.debug("wait redis lock {}", lockKey);
        do {
            Utils.sleep(100);
        } while (redis.isLocked(lockKey));
        logger.debug("got  redis lock {}", lockKey);
    }
}
