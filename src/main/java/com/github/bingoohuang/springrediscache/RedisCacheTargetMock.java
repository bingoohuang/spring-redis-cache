package com.github.bingoohuang.springrediscache;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCacheTargetMock {
    String value();
}
