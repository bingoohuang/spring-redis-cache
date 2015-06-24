package com.github.bingoohuang.springrediscache;

public interface RedisCacheExpirationAware {
    /**
     * 过期秒数.
     *
     * @return expiration seconds.
     */
    long expirationSeconds();
}
