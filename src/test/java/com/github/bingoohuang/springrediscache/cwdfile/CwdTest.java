package com.github.bingoohuang.springrediscache.cwdfile;

import com.github.bingoohuang.springrediscache.Utils;
import com.github.bingoohuang.springrediscachetest.SpringConfig;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SpringConfig.class})
public class CwdTest {
    @Autowired
    CwdService cwdService;

    static File file = new File("Cache.CwdService.Millis.NoArgs");

    @BeforeClass
    public static void beforeClass() {
        if (file.exists()) file.delete();
    }

    @AfterClass
    public static void afterClass() {
        if (file.exists()) file.delete();
    }

    @Test
    public void test() throws IOException {
        long millis1 = cwdService.millis();
        long millis2 = cwdService.millis();
        assertThat(millis1, is(equalTo(millis2)));

        Utils.sleep(15000);

        Files.write("1", file, Charsets.UTF_8);

        millis2 = cwdService.millis();
        assertThat(millis1 < millis2, is(true));
    }
}
