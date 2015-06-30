package com.github.bingoohuang.springrediscache.cwdfile;

import com.github.bingoohuang.springrediscache.RedisCacheEnabled;
import org.springframework.stereotype.Service;

import static com.github.bingoohuang.springrediscache.RedisFor.CwdFileRefreshSeconds;

@Service
public class CwdService {
    @RedisCacheEnabled(redisFor = CwdFileRefreshSeconds)
    public long millis() {
        return System.currentTimeMillis();
    }
}
