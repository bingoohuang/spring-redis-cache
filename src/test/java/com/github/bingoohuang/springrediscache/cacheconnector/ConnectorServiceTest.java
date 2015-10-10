package com.github.bingoohuang.springrediscache.cacheconnector;

import com.github.bingoohuang.springrediscache.RedisCacheConnector;
import com.github.bingoohuang.springrediscachetest.RedisBeanConfig;
import com.github.bingoohuang.springrediscachetest.SpringConfig;
import com.github.bingoohuang.utils.redis.Redis;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SpringConfig.class, RedisBeanConfig.class})
public class ConnectorServiceTest {
    @Autowired
    ConnectorService connectorService;
    @Autowired
    Redis redis;

    @Test
    public void test1() {
        redis.del("Cache:ConnectorService:AccessToken:NoArgs");
        String accessToken = RedisCacheConnector.connectCache(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return connectorService.getAccessToken();
            }
        }, "DirectServiceTest");

        String accessToken1 = connectorService.getAccessToken();
        assertThat(accessToken, is(equalTo(accessToken1)));
        assertThat(accessToken, is(equalTo("DirectServiceTest")));

        accessToken = RedisCacheConnector.connectCache(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return connectorService.getAccessToken();
            }
        }, "DirectServiceCacheValue");

        accessToken1 = connectorService.getAccessToken();
        assertThat(accessToken, is(equalTo(accessToken1)));
        assertThat(accessToken, is(equalTo("DirectServiceCacheValue")));

        accessToken = RedisCacheConnector.connectCache(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return connectorService.getAccessToken();
            }
        }, null);
        assertThat(accessToken, is(nullValue()));

        accessToken1 = connectorService.getAccessToken();
        assertThat(accessToken1, is(equalTo("Ignored")));
    }

    @Test
    public void test2() {
        String key = "Cache:ConnectorService:AccessTokenForClear:NoArgs";
        redis.del(key);
        String accessToken = connectorService.getAccessTokenForClear();
        String redisValue = redis.get(key);
        assertThat(redisValue, is(equalTo(accessToken)));

        RedisCacheConnector.clearCache(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return connectorService.getAccessTokenForClear();
            }
        });

        String accessToken2 = connectorService.getAccessTokenForClear();
        assertThat(accessToken2, not(equalTo(accessToken)));
        String redisValue2 = redis.get(key);
        assertThat(redisValue2, is(equalTo(accessToken2)));

        redis.set(key, "xxx");
        RedisCacheConnector.clearCache(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return connectorService.getAccessTokenForClear();
            }
        });
        String accessToken3 = connectorService.getAccessTokenForClear();
        assertThat(accessToken3, is(equalTo("xxx")));
    }
}
