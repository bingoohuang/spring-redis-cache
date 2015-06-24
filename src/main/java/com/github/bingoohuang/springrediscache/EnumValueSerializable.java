package com.github.bingoohuang.springrediscache;

import com.google.common.base.Enums;

import java.lang.reflect.Method;

class EnumValueSerializable implements ValueSerializable<Object> {
    private final Class<? extends Enum> returnType;

    public EnumValueSerializable(Class<?> returnType) {
        this.returnType = (Class<? extends Enum>) returnType;
    }

    @Override
    public String serialize(Object value) {
        return value.toString();
    }

    @Override
    public Object deserialize(String redisValue, Method method) {
        return Enums.stringConverter(returnType).convert(redisValue);
    }
}
