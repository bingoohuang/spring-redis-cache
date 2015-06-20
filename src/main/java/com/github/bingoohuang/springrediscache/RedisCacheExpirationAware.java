package com.github.bingoohuang.springrediscache;

public interface RedisCacheExpirationAware {
    long expirationMillis();
}
