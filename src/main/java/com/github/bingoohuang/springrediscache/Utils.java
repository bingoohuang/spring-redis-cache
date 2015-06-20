package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.redis.Redis;

class Utils {
    public static long redisExpiration(String key, Redis redis) {
        String expirationStr = redis.get(key);
        long expiration = Consts.MaxMillis;
        if (expirationStr == null) return expiration;
        if (expirationStr.matches("\\d+")) return Consts.MinMillis + Long.parseLong(expirationStr);
        return Consts.MinMillis + expirationStr.hashCode();
    }

}
