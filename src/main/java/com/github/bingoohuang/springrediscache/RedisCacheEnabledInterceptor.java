package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.redis.Redis;
import net.jodah.expiringmap.ExpiringMap;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RedisCacheEnabledInterceptor implements MethodInterceptor {
    @Autowired
    Redis redis;
    @Autowired
    ApplicationContext appContext;

    final ExpiringMap<String, CachedValueWrapper> cache = ExpiringMap.builder().variableExpiration().build();
    final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public RedisCacheEnabledInterceptor() {
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                checkCache();
            }
        }, Consts.RefreshSpanMillis, Consts.RefreshSpanMillis, TimeUnit.MILLISECONDS);
    }

    private void checkCache() {
        for (String key : cache.keySet()) {
            CachedValueWrapper wrapper = cache.get(key);
            boolean isRedisNotify = !wrapper.getRedisCacheAnn().redisStore();
            if (isRedisNotify) {
                long expiration = Utils.redisExpiration(key, redis);
                long cacheExpiration = cache.getExpiration(key);
                if (expiration != cacheExpiration) {
                    cache.remove(key);
                    Logger logger = wrapper.getLogger();
                    logger.debug("invalidate cache {} because of redis notifcation {} ", key, expiration);
                }
            }
        }
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        return new InvocationRuntime(invocation, redis, cache, appContext, executorService).process();
    }
}