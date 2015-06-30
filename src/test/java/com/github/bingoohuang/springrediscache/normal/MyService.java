package com.github.bingoohuang.springrediscache.normal;

import com.github.bingoohuang.springrediscache.RedisCacheEnabled;
import org.springframework.stereotype.Service;

import static com.github.bingoohuang.springrediscache.RedisFor.RefreshSeconds;

@Service
public class MyService {
    @RedisCacheEnabled(expirationSeconds = 2, aheadRefresh = true)
    public String getTokenRedisStore(String tokenId) {
        return tokenId + ":" + System.currentTimeMillis();
    }

    @RedisCacheEnabled(redisFor = RefreshSeconds)
    public String getTokenRedisRefresh(String tokenId) {
        return tokenId + ":" + System.currentTimeMillis();
    }

    @RedisCacheEnabled(expirationSeconds = 2, aheadRefresh = true, naming = MyServiceRedisCacheNameGenerator.class)
    public String getTokenRedisNaming(String tokenId) {
        return tokenId + ":" + System.currentTimeMillis();
    }
}
