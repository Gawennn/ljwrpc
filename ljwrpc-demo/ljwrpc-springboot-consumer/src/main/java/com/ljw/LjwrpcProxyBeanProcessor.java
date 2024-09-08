package com.ljw;

import com.ljw.annotation.LjwrpcService;
import com.ljw.proxy.LjwrpcProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Component
public class LjwrpcProxyBeanProcessor implements BeanPostProcessor {

    // 他会拦截所有的bean的创建，会在每一个bean的初始化后被调用
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // 想办法给他生成一个代理
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            LjwrpcService ljwrpcService = field.getAnnotation(LjwrpcService.class);
            if (ljwrpcService != null){
                // 获取一个代理
                Class<?> type = field.getType();
                Object proxy = LjwrpcProxyFactory.getProxy(type);
                field.setAccessible(true);
                try {
                    field.set(bean, proxy);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return bean;
    }
}
