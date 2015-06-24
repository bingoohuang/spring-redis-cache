package com.github.bingoohuang.springrediscache;

public enum RedisFor {
    /**
     * Redis用于存储值（值有过期设置）.
     */
    StoreValue,
    /**
     * Redis用于存储刷新时间戳，当刷新时间戳变化时，刷新本地缓存值.
     */
    RefreshSeconds
}
