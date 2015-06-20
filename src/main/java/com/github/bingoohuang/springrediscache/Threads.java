package com.github.bingoohuang.springrediscache;

import java.util.concurrent.TimeUnit;

public class Threads {
    public static void sleep(int milis) {
        try {
            TimeUnit.MILLISECONDS.sleep(milis);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
