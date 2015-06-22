package com.github.bingoohuang.springrediscache;

import org.slf4j.Logger;

class CachedValueWrapper {
    public static CachedValueWrapper instance = new CachedValueWrapper(null, null, null);

    private final Object value;
    private final RedisCacheEnabled redisCacheAnn;
    private Logger logger;

    CachedValueWrapper(Object value, RedisCacheEnabled redisCacheAnn, Logger logger) {
        this.value = value;
        this.redisCacheAnn = redisCacheAnn;
        this.logger = logger;
    }


    public Object getValue() {
        return value;
    }

    public RedisCacheEnabled getRedisCacheAnn() {
        return redisCacheAnn;
    }

    public Logger getLogger() {
        return logger;
    }
}
