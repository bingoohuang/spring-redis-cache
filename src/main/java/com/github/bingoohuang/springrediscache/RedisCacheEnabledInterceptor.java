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

import static com.github.bingoohuang.springrediscache.RedisFor.RefreshSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class RedisCacheEnabledInterceptor implements MethodInterceptor, Runnable {
    @Autowired
    Redis redis;
    @Autowired
    ApplicationContext appContext;

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

        this.executorService.scheduleAtFixedRate(this,
                Consts.RefreshSpanSeconds, Consts.RefreshSpanSeconds, SECONDS);
    }

    @Override
    public void run() {
        for (String key : cache.keySet()) {
            CachedValueWrapper wrapper = cache.get(key);
            if (wrapper.getRedisCacheAnn().redisFor() != RefreshSeconds) continue;

            long expiration = Utils.redisExpirationSeconds(key, redis);
            long cacheExpiration = cache.getExpiration(key);
            if (expiration == cacheExpiration) continue;

            CachedValueWrapper remove = cache.remove(key);
            if (remove == null) continue;

            Logger logger = wrapper.getLogger();
            logger.debug("invalidate cache {} because of redis notification {} ", key, expiration);
        }
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        return new InvocationRuntime(invocation,
                redis, cache, appContext, executorService).process();
    }
}