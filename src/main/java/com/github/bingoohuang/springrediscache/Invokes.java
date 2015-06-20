package com.github.bingoohuang.springrediscache;

import org.aopalliance.intercept.MethodInvocation;

public class Invokes {
    public static Object invokeMethod(MethodInvocation invocation) {
        try {
            return  invocation.proceed();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
