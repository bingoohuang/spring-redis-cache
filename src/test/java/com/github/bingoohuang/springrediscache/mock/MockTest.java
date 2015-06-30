package com.github.bingoohuang.springrediscache.mock;

import com.github.bingoohuang.springrediscachetest.RedisBeanConfig;
import com.github.bingoohuang.springrediscachetest.SpringConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SpringConfig.class, RedisBeanConfig.class})
public class MockTest {
    @Autowired
    MockedService mockedService;
    @Autowired
    RealService realService;


    @Test
    public void mock() {
        mockedService.setMocked(31415926L);
        assertThat(realService.millis(), is(equalTo(31415926L)));
    }
}
