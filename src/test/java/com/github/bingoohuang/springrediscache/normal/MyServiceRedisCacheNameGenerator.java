package com.github.bingoohuang.springrediscache.normal;

import com.github.bingoohuang.springrediscache.RedisCacheNameGenerator;
import com.github.bingoohuang.springrediscache.Utils;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

@Component
public class MyServiceRedisCacheNameGenerator implements RedisCacheNameGenerator {
    @Override
    public String generateCacheName(MethodInvocation invocation) {
        return "Bingoo:" + Utils.joinArguments(invocation);
    }
}
