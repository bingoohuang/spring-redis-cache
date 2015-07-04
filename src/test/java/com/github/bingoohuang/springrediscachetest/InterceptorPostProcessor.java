package com.github.bingoohuang.springrediscachetest;

import com.github.bingoohuang.springrediscache.RedisCacheEnabledInterceptor;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class InterceptorPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RedisCacheEnabledInterceptor) {
            new BeanWrapperImpl(bean).setPropertyValue("refreshSpanSeconds", 5L);
        }

        return bean;
    }

}
