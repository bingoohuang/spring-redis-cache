package com.github.bingoohuang.springrediscache;

import org.aopalliance.intercept.MethodInvocation;

public interface RedisCacheNameGenerator {
    String generateCacheName(MethodInvocation invocation);
}
