package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.redis.Redis;
import com.github.bingoohuang.utils.redis.RedisOp;
import net.jodah.expiringmap.ExpiringMap;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.github.bingoohuang.springrediscache.RedisCacheUtils.redisExpirationSeconds;
import static com.github.bingoohuang.springrediscache.RedisFor.StoreValue;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class RedisCacheEnabledInterceptor implements MethodInterceptor, Runnable {
    @Autowired
    ApplicationContext appContext;
    long refreshSpanSeconds = Consts.RefreshSpanSeconds;

    final ExpiringMap<String, CachedValueWrapper> cache;
    final ScheduledExecutorService executorService;

    public RedisCacheEnabledInterceptor() {
        this(Executors.newSingleThreadScheduledExecutor(),
                ExpiringMap.builder().variableExpiration().<String, CachedValueWrapper>build());
    }

    public RedisCacheEnabledInterceptor(ScheduledExecutorService executorService,
                                        ExpiringMap<String, CachedValueWrapper> cache) {
        this.cache = cache;
        this.executorService = executorService;

    }

    @PostConstruct
    public void postContruct() {
        executorService.scheduleAtFixedRate(this, refreshSpanSeconds, refreshSpanSeconds, SECONDS);
    }

    @PreDestroy
    public void cleanUp() throws Exception {
        this.executorService.shutdown();
    }

    @Override
    public void run() {
        int reties = 3;
        while (reties-- > 0) {
            try {
                scanEntries();
                break;
            } catch (ConcurrentModificationException e) {
                RedisCacheUtils.sleep(100);
                continue;
            }
        }
    }

    private void scanEntries() {
        for (final String key : cache.keySet()) {
            final CachedValueWrapper wrapper = cache.get(key);
            if (wrapper.getRedisCacheAnn().redisFor() == StoreValue) {
                checkKeyExistsInRedis(key, wrapper);
            } else {
                checkRefreshTimestampInRedis(key, wrapper);
            }
        }
    }

    private void checkRefreshTimestampInRedis(String key, CachedValueWrapper wrapper) {
        long expiration = redisExpirationSeconds(key, appContext);
        long cacheExpiration = cache.getExpiration(key);

        if (expiration == cacheExpiration) return;

        CachedValueWrapper removed = cache.remove(key);
        if (removed == null) return;

        Logger logger = wrapper.getLogger();
        logger.debug("invalidate cache {} because of redis refresh seconds " +
                "changed to {} ", key, expiration);
    }

    private void checkKeyExistsInRedis(final String key, final CachedValueWrapper wrapper) {
        Redis redis = RedisCacheUtils.tryGetBean(appContext, Redis.class);
        RedisOp redisOp = new RedisOp() {
            @Override
            public Object exec(Jedis jedis) {
                if (jedis.exists(key)) return null;

                CachedValueWrapper removed = cache.remove(key);
                if (removed != null) return null;

                Logger logger = wrapper.getLogger();
                logger.debug("invalidate cache {} because of redis key non-exists ", key);

                return null;
            }
        };
        redis.op(redisOp);
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        return new InvocationRuntime(invocation,
                cache, appContext, executorService).process();
    }
}