package com.ljw.config;

import com.ljw.compress.Compressor;
import com.ljw.compress.CompressorFactory;
import com.ljw.loadbalancer.LoadBalancer;
import com.ljw.serialize.Serializer;
import com.ljw.serialize.SerializerFactory;
import com.ljw.spi.SpiHandler;

import java.util.List;

/**
 * 使用 SPI 实现进行系统配置和初始化
 *
 * @author 刘家雯
 * @version 1.0
 */
public class SpiResolver {

    /**
     * 通过spi的方式加载配置项
     * @param configuration 配置上下文
     */
    public void loadFromSpi(Configuration configuration) {

        // 1.我的spi的文件中配置了很多实现（自由定义，只能配置一个实现，还是多个）
        List<ObjectWrapper<LoadBalancer>> loadBalancerWrappers = SpiHandler.getList(LoadBalancer.class);
        // 将其放入工厂
        if (loadBalancerWrappers != null && loadBalancerWrappers.size() > 0) {
            configuration.setLoadBalancer(loadBalancerWrappers.get(0).getImpl());
        }

        List<ObjectWrapper<Compressor>> compressorobjectWrappers = SpiHandler.getList(Compressor.class);
        if (compressorobjectWrappers != null) {
            compressorobjectWrappers.forEach(CompressorFactory::addCompressor);
        }

        List<ObjectWrapper<Serializer>> serializerobjectWrappers = SpiHandler.getList(Serializer.class);
        if (serializerobjectWrappers != null) {
            serializerobjectWrappers.forEach(SerializerFactory::addSerializer);
        }
    }
}
