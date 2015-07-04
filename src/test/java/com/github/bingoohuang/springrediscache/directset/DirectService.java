package com.github.bingoohuang.springrediscache.directset;

import com.github.bingoohuang.springrediscache.RedisCacheEnabled;
import com.github.bingoohuang.springrediscache.RedisFor;
import org.springframework.stereotype.Service;

@Service
public class DirectService {
    @RedisCacheEnabled(redisFor = RedisFor.StoreValue, expirationSeconds = 24 * 60 * 60)
    public String getAccessToken() {
        return "Ignored";
    }
}
