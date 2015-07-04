package com.github.bingoohuang.springrediscache.customexpire;

import com.github.bingoohuang.springrediscache.RedisCacheExpirationAwareTag;

public class CustomExpireBean2 {
    private long value;
    private int expirationSeconds;

    public CustomExpireBean2() {
    }

    public CustomExpireBean2(long value, int expireInSeconds) {
        this.value = value;
        this.expirationSeconds = expireInSeconds;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void setExpirationSeconds(int expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }

    public long getValue() {
        return value;
    }

    @RedisCacheExpirationAwareTag
    public int getExpirationSeconds() {
        return expirationSeconds;
    }
}
