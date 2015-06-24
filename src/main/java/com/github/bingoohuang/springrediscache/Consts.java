package com.github.bingoohuang.springrediscache;

interface Consts {
    long DaySeconds = 24L * 60 * 60;
    long RefreshSpanSeconds = 15;
    long MaxSeconds = Long.MAX_VALUE / 1000 / 1000 / 1000;
    long MinSeconds = Integer.MAX_VALUE;
    long AheadRefreshSeconds = 60L;
}
