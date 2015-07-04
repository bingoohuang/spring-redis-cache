package com.github.bingoohuang.springrediscache;

import net.jodah.expiringmap.ExpiringMap;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.github.bingoohuang.springrediscache.RedisFor.StoreValue;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class RedisCacheEnabledInterceptor implements MethodInterceptor, Runnable {
    @Autowired ApplicationContext appContext;
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
        for (String key : cache.keySet()) {
            CachedValueWrapper wrapper = cache.get(key);
            if (wrapper.getRedisCacheAnn().redisFor() == StoreValue) continue;

            long expiration = RedisCacheUtils.redisExpirationSeconds(key, appContext);
            long cacheExpiration = cache.getExpiration(key);

            if (expiration == cacheExpiration) continue;

            CachedValueWrapper removed = cache.remove(key);
            if (removed == null) continue;

            Logger logger = wrapper.getLogger();
            logger.debug("invalidate cache {} because of redis refresh seconds changed to {} ", key, expiration);
        }
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        return new InvocationRuntime(invocation,
                cache, appContext, executorService).process();
    }
}