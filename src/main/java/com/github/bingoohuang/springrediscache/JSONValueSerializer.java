package com.github.bingoohuang.springrediscache;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

class JSONValueSerializer implements ValueSerializable {
    private final Class<?> returnType;
    private final Logger logger;

    public JSONValueSerializer(Class<?> returnType, Logger logger) {
        this.returnType = returnType;
        this.logger = logger;
    }

    @Override
    public String serialize(Object value) {
        return JSON.toJSONString(value);
    }

    @Override
    public Object deserialize(String redisValue, Method method) {
        try {
            if (Map.class.isAssignableFrom(returnType)) {
                return JSON.parseObject(redisValue, Map.class);
            }

            if (List.class.isAssignableFrom(returnType)) {
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    Type type = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
                    return JSON.parseArray(redisValue, (Class<?>) type);
                }

            }
            return JSON.parseObject(redisValue, returnType);
        } catch (Exception e) {
            logger.error("unable to deserialize " + redisValue, e);
            return null;
        }
    }
}
