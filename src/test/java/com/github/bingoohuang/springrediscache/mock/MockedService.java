package com.github.bingoohuang.springrediscache.mock;

import org.springframework.stereotype.Component;

@Component
public class MockedService {
    private long mocked;

    public long millis() {
        return mocked;
    }

    public void setMocked(long mocked) {
        this.mocked = mocked;
    }
}
