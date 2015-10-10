package com.github.bingoohuang.springrediscache.normal;


import com.github.bingoohuang.springrediscache.RedisCacheEnabled;
import org.springframework.stereotype.Service;

@Service
public class RedisStoreService {
    @RedisCacheEnabled
    public String getToken(String tokenId) {
        return tokenId + ":" + System.currentTimeMillis();
    }
}
