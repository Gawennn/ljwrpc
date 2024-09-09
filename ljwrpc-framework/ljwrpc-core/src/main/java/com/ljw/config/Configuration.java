package com.ljw.config;

import com.ljw.discovery.RegistryConfig;
import com.ljw.loadbalancer.LoadBalancer;
import com.ljw.loadbalancer.impl.RoundRobinLoadBalancer;
import com.ljw.protection.CircuitBreaker;
import com.ljw.protection.RateLimiter;
import com.ljw.IdGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 全局的配置类，代码配置--> xml配置--> 默认项
 *
 * 当在 LjwrpcBootstrap 中调用 serialize("hessian") 时，它会直接覆盖 Configuration 类中 serializeType 的值。
 * 由于这个调用发生在构造函数之后，所以最终生效的是代码中的动态配置
 * 换句话说，构造函数加载 XML 配置后，如果通过 LjwrpcBootstrap.serialize 手动指定序列化方式（如 "hessian"），
 * 那么会覆盖构造函数中从 XML 加载的值
 *
 * 任何配置如代码配置、xml配置、spi配置都是通过修改这个全局配置类来完成的
 *
 * @author 刘家雯
 * @version 1.0
 */
@Data
@Slf4j
public class Configuration {

    // 配置信息-->端口号
    private int port = 8088;

    // 配置信息-->应用的程序名字
    private String appName = "default";

    // 分组信息
    private String group = "default";

    // 配置信息-->注册中心
    private RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");

    // 配置信息-->序列化协议
    private String serializeType = "jdk";

    // 配置信息-->压缩协议
    private String compressType = "gzip";

    // 配置信息-->ID生成器
    public IdGenerator idGenerator = new IdGenerator(1, 2);

    // 配置信息-->负载均衡策略
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    // 为每一个ip配置一个限流器
    private final Map<SocketAddress, RateLimiter> everyIpRateLimiter = new ConcurrentHashMap<>(16);

    // 为每一个ip配置一个断路器，熔断
    private final Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = new ConcurrentHashMap<>(16);


    // 读xml, dom4j
    public Configuration() {

        // spi机制发现相关配置项
        SpiResolver spiResolver = new SpiResolver();
        spiResolver.loadFromSpi(this);

        // 通过读取xml获取上面的信息
        XmlResolver xmlResolver = new XmlResolver();
        xmlResolver.loadFromXml(this);
    }
}