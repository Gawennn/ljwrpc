package com.ljw.config;

import com.ljw.ProtocolConfig;
import com.ljw.compress.Compressor;
import com.ljw.compress.impl.GzipCompressor;
import com.ljw.discovery.RegistryConfig;
import com.ljw.loadbalancer.LoadBalancer;
import com.ljw.loadbalancer.impl.RoundRobinLoadBalancer;
import com.ljw.serialize.Serializer;
import com.ljw.serialize.impl.JdkSerializer;
import com.ljw.utils.IdGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;


/**
 * 全局的配置类，代码配置--> xml配置--> 默认项
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

    // 配置信息-->注册中心
    private RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");

    // 配置信息-->序列化协议
    private String serializeType = "jdk";

    // 配置信息-->压缩协议
    private String compressType = "gzip";

    // 配置信息-->ID生成器
    private final IdGenerator idGenerator = new IdGenerator(1,2);

    // 配置信息-->负载均衡策略
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    // 读xml, dom4j
    public Configuration() {
        // 1.成员变量的默认配置项

        // 2.spi机制发现相关配置项
        SpiResolver spiResolver = new SpiResolver();
        spiResolver.loadFromSpi(this);

        // 3.通过读取xml获取上面的信息
        XmlResolver xmlResolver = new XmlResolver();
        xmlResolver.loadFromXml(this);

        // 4.编程配置项，ljwrpcBootstrap提供
    }

    public static void main(String[] args) {
        Configuration configuration = new Configuration();
    }

}
