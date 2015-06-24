package com.github.bingoohuang.springrediscache;

class RefreshMillisProcessor implements CacheProcessor {
    private final InvocationRuntime runtime;

    RefreshMillisProcessor(InvocationRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Object process() {
        Object value = runtime.getLocalCache();
        if (value != null) return value;

        try {
            runtime.waitCacheLock();

            // check again
            value = runtime.getLocalCache();
            if (value != null) return value;

            return runtime.invokeMethodAndPutCache();
        } finally {
            runtime.unlockLocalCache();
        }
    }
}
