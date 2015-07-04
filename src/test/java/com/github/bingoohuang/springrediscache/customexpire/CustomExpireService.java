package com.github.bingoohuang.springrediscache.customexpire;

import com.github.bingoohuang.springrediscache.RedisCacheEnabled;
import org.springframework.stereotype.Service;

@Service
public class CustomExpireService {
    @RedisCacheEnabled
    public CustomExpireBean millis() {
        return new CustomExpireBean(System.currentTimeMillis(), 60);
    }
}
