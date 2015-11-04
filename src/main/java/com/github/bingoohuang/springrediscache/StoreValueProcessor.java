package com.github.bingoohuang.springrediscache;

class StoreValueProcessor implements CacheProcessor {
    private final InvocationRuntime rt;

    StoreValueProcessor(InvocationRuntime rt) {
        this.rt = rt;
    }

    @Override
    public Object process() {
        Object value = rt.getLocalCache();
        if (value != null) {
            tryRefreshAhead();
            return value;
        }

        try {
            rt.waitCacheLock();
            value = rt.getLocalCache();
            if (value != null) return value;

            return forceRefresh();
        } finally {
            rt.unlockLocalCache();
        }
    }

    private void tryRefreshAhead() {
        if (!rt.isAheadRefreshEnabled()) return;
        if (rt.isBeforeAheadSeconds()) return;

        if (!rt.tryLockLocalCache()) return;

        rt.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    forceRefresh();
                } finally {
                    rt.unlockLocalCache();
                }
            }
        });
    }

    private Object forceRefresh() {
        long ttlSeconds = rt.redisTtlSeconds();

        if (ttlSeconds > getAheadSeconds()) {
            rt.loadRedisValueToCache(ttlSeconds);
        }

        if (rt.getValue() == null) tryRefreshOrReadRedis();

        return rt.getValue();
    }

    private long getAheadSeconds() {
        return rt.isAheadRefreshEnabled() ? Consts.AheadRefreshSeconds : 0;
    }

    private void tryRefreshOrReadRedis() {
        boolean lock = false;
        try {
            lock = rt.tryRedisLock();
            if (lock) {
                boolean ok = rt.loadRedisValueToCache(-1); // try read again
                if (!ok) invokeMethodAndSaveCache();
                return;
            }
        } finally {
            rt.unlockRedis(lock);
        }

        rt.waitLockReleaseAndReadRedis();
    }

    private void invokeMethodAndSaveCache() {
        Object cachedValue = rt.invokeMethod();
        if (cachedValue != null) {
            long expirationSeconds = RedisCacheUtils.getExpirationSeconds(rt);

            rt.setex(expirationSeconds);
            rt.putLocalCache(expirationSeconds);
        }
    }

}
