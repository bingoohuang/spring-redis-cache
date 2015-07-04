package com.github.bingoohuang.springrediscache.customexpire;

import com.github.bingoohuang.springrediscache.RedisCacheEnabled;
import org.springframework.stereotype.Service;

@Service
public class CustomExpireService2 {
    @RedisCacheEnabled
    public CustomExpireBean2 millis() {
        return new CustomExpireBean2(System.currentTimeMillis(), 60);
    }
}
