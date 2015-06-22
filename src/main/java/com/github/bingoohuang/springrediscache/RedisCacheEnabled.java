package com.github.bingoohuang.springrediscache;

import java.lang.annotation.*;


/**
 * 激活方法返回值的缓存.
 */
@Inherited
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCacheEnabled {
    /**
     * 缓存前缀名称前缀.
     * 缓存名称，例如：Cache:MerchantService:MerchantID.
     *
     * @return 缓存名称前缀.
     */
    String prefix() default "Cache";

    /**
     * 缓存过期微秒数.
     * 在方法方法类型实现了@CacheExpirationAware时，可以不设置;否则必须设置为大于0，小于24*60*60*1000(一天).
     *
     * @return 缓存过期微秒数.
     */
    long expirationMillis() default -1;

    /**
     * 提前刷新微秒数.
     * 由此数字决定缓存是否快要过期，快要过期时，提前刷新缓存.
     * 必须小于60*1000.
     *
     * @return 提前刷新微秒数.
     */
    long aheadMillis() default 10000;

    /**
     * 缓存值是否在redis中存取.
     *
     * @return true 在redis中存取缓存值，并且同步过期时间.
     * false 不在redis中存取缓存值，只在redis中存取是否需要刷新.
     */
    boolean redisStore() default true;

    /**
     * 自定义缓存名称生成器.
     *
     * @return 缓存名称生成器.
     */
    Class<? extends RedisCacheNameGenerator> naming() default NoopRedisCacheNameGenerator.class;
}