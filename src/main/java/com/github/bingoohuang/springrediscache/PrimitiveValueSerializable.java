package com.github.bingoohuang.springrediscache;

import java.lang.reflect.Method;

class PrimitiveValueSerializable implements ValueSerializable<Object> {
    private final Class<?> returnType;

    public PrimitiveValueSerializable(Class<?> returnType) {
        this.returnType = returnType;
    }

    @Override
    public String serialize(Object value) {
        return value.toString();
    }

    @Override
    public Object deserialize(String value, Method method) {
        if (Boolean.class == returnType || boolean.class == returnType) return Boolean.parseBoolean(value);
        if (Byte.class == returnType || byte.class == returnType) return Byte.parseByte(value);
        if (Short.class == returnType || short.class == returnType) return Short.parseShort(value);
        if (Integer.class == returnType || int.class == returnType) return Integer.parseInt(value);
        if (Long.class == returnType || long.class == returnType) return Long.parseLong(value);
        if (Float.class == returnType || float.class == returnType) return Float.parseFloat(value);
        if (Double.class == returnType || double.class == returnType) return Double.parseDouble(value);

        throw new RuntimeException("unsupported prmitive type " + returnType);
    }
}
