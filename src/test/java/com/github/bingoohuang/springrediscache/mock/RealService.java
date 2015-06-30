package com.github.bingoohuang.springrediscache.mock;

import com.github.bingoohuang.springrediscache.RedisCacheEnabled;
import com.github.bingoohuang.springrediscache.RedisCacheTargetMock;
import org.springframework.stereotype.Service;

import static com.github.bingoohuang.springrediscache.RedisFor.RefreshSeconds;

@Service
@RedisCacheTargetMock(value = "com.github.bingoohuang.springrediscache.mock.MockedService")
public class RealService {
    @RedisCacheEnabled(redisFor = RefreshSeconds)
    public long millis() {
        return System.currentTimeMillis();
    }
}
