package com.github.bingoohuang.springrediscache.directset;

import com.github.bingoohuang.springrediscache.RedisCacheConnecter;
import com.github.bingoohuang.springrediscachetest.SpringConfig;
import com.github.bingoohuang.utils.redis.Redis;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SpringConfig.class})
public class DirectServiceTest {
    @Autowired
    DirectService directService;
    @Autowired
    Redis redis;

    @Test
    public void test1() {
        redis.del("Cache:DirectService:AccessToken:NoArgs");
        String accessToken = RedisCacheConnecter.connectCache(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return directService.getAccessToken();
            }
        }, "DirectServiceTest");

        String accessToken1 = directService.getAccessToken();
        assertThat(accessToken, is(equalTo(accessToken1)));
        assertThat(accessToken, is(equalTo("DirectServiceTest")));
    }
}
