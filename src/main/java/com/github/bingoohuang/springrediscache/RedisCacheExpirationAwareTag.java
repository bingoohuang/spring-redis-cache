package com.github.bingoohuang.springrediscache;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCacheExpirationAwareTag {
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
