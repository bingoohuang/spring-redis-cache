package com.github.bingoohuang.springrediscache;

import java.lang.reflect.Method;

class StringValueSerializable implements ValueSerializable<String> {
    @Override
    public String serialize(String value) {
        return value;
    }

    @Override
    public String deserialize(String redisValue, Method method) {
        return redisValue;
    }
}
