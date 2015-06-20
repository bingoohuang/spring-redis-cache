package com.github.bingoohuang.springrediscache;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

@Component
public class MyServiceCacheNameGenerator implements CacheNameGenerator {
    @Override
    public String generateCacheName(MethodInvocation invocation) {
        return "Bingoo:" + Args.joinArguments(invocation);
    }
}
