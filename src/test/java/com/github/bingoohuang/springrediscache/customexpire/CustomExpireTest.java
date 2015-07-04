package com.github.bingoohuang.springrediscache.customexpire;

import com.github.bingoohuang.springrediscachetest.SpringConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SpringConfig.class})
public class CustomExpireTest {
    @Autowired
    CustomExpireService service;
    @Autowired
    CustomExpireService2 service2;


    @Test
    public void test() throws IOException {
        CustomExpireBean millis1 = service.millis();
        CustomExpireBean millis2 = service.millis();
        assertThat(millis1.getValue(), is(equalTo(millis2.getValue())));
    }

    @Test
    public void test2() throws IOException {
        CustomExpireBean2 millis1 = service2.millis();
        CustomExpireBean2 millis2 = service2.millis();
        assertThat(millis1.getValue(), is(equalTo(millis2.getValue())));
    }
}
