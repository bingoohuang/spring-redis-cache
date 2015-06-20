package com.github.bingoohuang.springrediscache;

import org.aopalliance.intercept.MethodInvocation;

public interface CacheNameGenerator {
    String generateCacheName(MethodInvocation invocation);
}
