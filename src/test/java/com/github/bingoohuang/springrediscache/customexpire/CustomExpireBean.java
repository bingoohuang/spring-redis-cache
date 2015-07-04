package com.github.bingoohuang.springrediscache.customexpire;

import com.github.bingoohuang.springrediscache.RedisCacheExpirationAware;

public class CustomExpireBean implements RedisCacheExpirationAware {
    private long value;
    private int expirationSeconds;

    public CustomExpireBean() {
    }

    public CustomExpireBean(long value, int expireInSeconds) {
        this.value = value;
        this.expirationSeconds = expireInSeconds;
    }

    @Override
    public long expirationSeconds() {
        return expirationSeconds;
    }

    public long getValue() {
        return value;
    }


    public int getExpirationSeconds() {
        return expirationSeconds;
    }
}
