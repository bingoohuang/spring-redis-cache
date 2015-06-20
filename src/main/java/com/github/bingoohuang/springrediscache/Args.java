package com.github.bingoohuang.springrediscache;

import com.github.bingoohuang.utils.codec.Json;
import com.google.common.primitives.Primitives;
import org.aopalliance.intercept.MethodInvocation;

public class Args {
    public static String joinArguments(MethodInvocation invocation) {
        StringBuilder sb = new StringBuilder();

        for (Object arg : invocation.getArguments()) {
            sb.append(convert(arg)).append(':');
        }

        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static String convert(Object arg) {
        if (arg == null) return null;

        if (arg.getClass().isPrimitive()) return arg.toString();
        if (Primitives.isWrapperType(arg.getClass())) return arg.toString();
        if (arg instanceof Enum) return arg.toString();
        if (arg instanceof CharSequence) return arg.toString();

        return Json.json(arg);
    }
}
