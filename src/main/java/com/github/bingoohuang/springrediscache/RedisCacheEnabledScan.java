package com.github.bingoohuang.springrediscache;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RedisCacheSpringConfig.class)
public @interface RedisCacheEnabledScan {
}
