package com.github.bingoohuang.springrediscache.normal;

import com.github.bingoohuang.springrediscache.RedisCacheUtils;
import com.github.bingoohuang.springrediscachetest.RedisBeanConfig;
import com.github.bingoohuang.springrediscachetest.SpringConfig;
import com.github.bingoohuang.utils.redis.Redis;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.google.common.truth.Truth.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SpringConfig.class, RedisBeanConfig.class})
public class RedisStoreTest {
    @Autowired
    Redis redis;
    @Autowired
    RedisStoreService redisStoreService;


    @Test
    public void test1() {
        String abc = redisStoreService.getToken("abc");
        String key = "Cache:RedisStoreService:Token:abc";
        String value = redis.get(key);
        assertThat(value).isEqualTo(abc);

        redis.del(key);
        RedisCacheUtils.sleep(15000);
        String abc2 = redisStoreService.getToken("abc");
        assertThat(abc2).isNotEqualTo(abc);
    }
}
