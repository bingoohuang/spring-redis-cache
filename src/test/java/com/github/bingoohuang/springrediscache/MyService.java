package com.github.bingoohuang.springrediscache;

import org.springframework.stereotype.Service;

@Service
public class MyService {
    @RedisCacheEnabled(expirationMillis = 2000, aheadMillis = 100)
    public String getTokenRedisStore(String tokenId) {
        return tokenId + ":" + System.currentTimeMillis();
    }

    @RedisCacheEnabled(expirationMillis = 3000, redisStore = false)
    public String getTokenRedisNotify(String tokenId) {
        return tokenId + ":" + System.currentTimeMillis();
    }

    @RedisCacheEnabled(expirationMillis = 2000, aheadMillis = 100, naming = MyServiceCacheNameGenerator.class)
    public String getTokenRedisNaming(String tokenId) {
        return tokenId + ":" + System.currentTimeMillis();
    }
}
