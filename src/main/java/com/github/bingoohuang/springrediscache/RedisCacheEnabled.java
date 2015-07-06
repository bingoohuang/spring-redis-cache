package com.github.bingoohuang.springrediscache;

import java.lang.annotation.*;

import static com.github.bingoohuang.springrediscache.RedisFor.StoreValue;


/**
 * 激活方法返回值的缓存.
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCacheEnabled {
    /**
     * 缓存过期秒数.
     * 当redisFor = StoreValue有效.
     * 在方法方法类型实现了@CacheExpirationAware时，可以不设置;否则必须设置为大于0，小于24*60*60(一天).
     *
     * @return 缓存过期微秒数.
     */
    long expirationSeconds() default 24 * 60 * 60;

    /**
     * 缓存在1分后过期时，是否提前刷新缓存.
     *
     * @return 是否提前刷新.
     */
    boolean aheadRefresh() default false;

    /**
     * Redis的用途.
     *
     * @return true 在redis中存取缓存值，并且同步过期时间.
     * false 不在redis中存取缓存值，只在redis中存取是否需要刷新.
     */
    RedisFor redisFor() default StoreValue;

    /**
     * 自定义缓存名称生成器.
     *
     * @return 缓存名称生成器.
     */
    Class<? extends RedisCacheNameGenerator> naming() default NoopRedisCacheNameGenerator.class;


    /**
     * 值存取到Redis中的序列化器.
     * 当redisFor = StoreValue有效.
     *
     * @return
     */
    Class<? extends ValueSerializable> valueSerializer() default AutoSelectValueSerializer.class;
}