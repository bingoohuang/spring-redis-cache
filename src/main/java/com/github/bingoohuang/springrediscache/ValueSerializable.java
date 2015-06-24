package com.github.bingoohuang.springrediscache;

import java.lang.reflect.Method;

public interface ValueSerializable<T> {
    String serialize(T value);

    T deserialize(String redisValue, Method method);
}
