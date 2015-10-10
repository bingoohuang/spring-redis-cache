package com.github.bingoohuang.springrediscache.cacheconnector;

import com.github.bingoohuang.springrediscache.RedisCacheEnabled;
import com.github.bingoohuang.springrediscache.RedisFor;
import org.springframework.stereotype.Service;

@Service
public class ConnectorService {
    @RedisCacheEnabled(redisFor = RedisFor.StoreValue)
    public String getAccessToken() {
        return "Ignored";
    }

    @RedisCacheEnabled(redisFor = RedisFor.StoreValue)
    public String getAccessTokenForClear() {
        return "" + System.currentTimeMillis();
    }
}
