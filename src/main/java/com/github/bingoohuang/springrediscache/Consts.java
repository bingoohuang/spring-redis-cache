package com.github.bingoohuang.springrediscache;

interface Consts {
    long DayMillis = 24L * 60 * 60 * 1000;
    long RefreshSpanMillis = 15 * 1000;
    long MaxMillis = Long.MAX_VALUE / 1000 / 1000;
    long MinMillis = Integer.MAX_VALUE;
}
