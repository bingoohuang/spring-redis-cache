package com.github.bingoohuang.springrediscache.normal;

import com.github.bingoohuang.springrediscache.RedisCacheUtils;
import com.github.bingoohuang.springrediscachetest.RedisBeanConfig;
import com.github.bingoohuang.springrediscachetest.SpringConfig;
import com.github.bingoohuang.utils.redis.Redis;
import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SpringConfig.class, RedisBeanConfig.class})
public class CacheTest {
    @Autowired
    MyService myService;
    @Autowired
    Redis redis;

    @BeforeClass
    public static void beforeClass() {
        Logger logger = LoggerFactory.getLogger(MyService.class);
        logger.debug("something");
    }

    @Test
    public void getTokenRedisNaming() {
        redis.del("Bingoo:bingoo1");
        String token1 = myService.getTokenRedisNaming("bingoo1");
        RedisCacheUtils.sleep(1000);
        String token2 = myService.getTokenRedisNaming("bingoo1");
        assertThat(token1, is(equalTo(token2)));
        RedisCacheUtils.sleep(2100);
        String token3 = myService.getTokenRedisNaming("bingoo1");
        assertTrue(token2.compareTo(token3) < 0);
    }

    @Test
    public void getTokenRedisStore() {
        redis.del("Cache:MyService:TokenRedisRefresh:bingoo1");
        String token1 = myService.getTokenRedisStore("bingoo1");
        RedisCacheUtils.sleep(1000);
        String token2 = myService.getTokenRedisStore("bingoo1");
        assertThat(token1, is(equalTo(token2)));
        RedisCacheUtils.sleep(2000);
        String token3 = myService.getTokenRedisStore("bingoo1");
        assertTrue(token2.compareTo(token3) < 0);

        redis.del("Cache:MyService:TokenRedisRefresh:bingoo11");
        String token11 = myService.getTokenRedisStore("bingoo11");
        RedisCacheUtils.sleep(1000);
        String token21 = myService.getTokenRedisStore("bingoo11");
        assertThat(token11, is(equalTo(token21)));
        RedisCacheUtils.sleep(2000);
        String token31 = myService.getTokenRedisStore("bingoo11");
        assertTrue(token21.compareTo(token31) < 0);
    }

    @Test
    public void getTokenRedisRefresh() {
        String token1 = myService.getTokenRedisRefresh("bingoo2");
        RedisCacheUtils.sleep(1000);
        String token2 = myService.getTokenRedisRefresh("bingoo2");
        assertThat(token1, is(equalTo(token2)));
        RedisCacheUtils.sleep(1000);
        String token3 = myService.getTokenRedisRefresh("bingoo2");
        assertThat(token3, is(equalTo(token2)));

        redis.incr("Cache:MyService:TokenRedisRefresh:bingoo2");
        RedisCacheUtils.sleep(15100); // at least 15 seconds
        String token4 = myService.getTokenRedisRefresh("bingoo2");
        assertTrue(token3.compareTo(token4) < 0);
    }


    public static class MyThreadRedisStore extends Thread {
        private final CyclicBarrier barrier;
        private final ArrayList<String> objects;
        private final MyService myService;

        public MyThreadRedisStore(CyclicBarrier barrier, ArrayList<String> objects, MyService myService) {
            this.barrier = barrier;
            this.objects = objects;
            this.myService = myService;
        }

        @Override
        public void run() {
            try {
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

            String token1 = myService.getTokenRedisStore("bingoo3");
            synchronized (objects) {
                objects.add(token1);
            }
        }
    }

    @Test
    public void getTokenRedisStoreMultiThreads() throws InterruptedException {

        final CyclicBarrier barrier = new CyclicBarrier(3);
        final ArrayList<String> objects = Lists.newArrayList();
        MyThreadRedisStore[] threads = new MyThreadRedisStore[3];
        for (int i = 0; i < 3; ++i) {
            threads[i] = new MyThreadRedisStore(barrier, objects, myService);
            threads[i].start();
        }

        for (int i = 0; i < 3; ++i) {
            threads[i].join();
        }


        assertThat(objects.get(0), is(equalTo(objects.get(1))));
        assertThat(objects.get(2), is(equalTo(objects.get(1))));
    }

    public static class MyThreadRedisNotify extends Thread {
        private final CyclicBarrier barrier;
        private final ArrayList<String> objects;
        private final MyService myService;

        public MyThreadRedisNotify(CyclicBarrier barrier, ArrayList<String> objects, MyService myService) {
            this.barrier = barrier;
            this.objects = objects;
            this.myService = myService;
        }

        @Override
        public void run() {
            try {
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

            String token1 = myService.getTokenRedisRefresh("bingoo4");
            synchronized (objects) {
                objects.add(token1);
            }
        }
    }

    @Test
    public void getTokenRedisNotifyMultiThreads() throws InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(3);
        final ArrayList<String> objects = Lists.newArrayList();
        MyThreadRedisNotify[] threads = new MyThreadRedisNotify[3];
        for (int i = 0; i < 3; ++i) {
            threads[i] = new MyThreadRedisNotify(barrier, objects, myService);
            threads[i].start();
        }

        for (int i = 0; i < 3; ++i) {
            threads[i].join();
        }

        assertThat(objects.get(0), is(equalTo(objects.get(1))));
        assertThat(objects.get(2), is(equalTo(objects.get(1))));
    }


}
