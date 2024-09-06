package com.ljw;

import com.ljw.annotation.LjwrpcApi;
import com.ljw.channelhandler.handler.LjwrpcRequestDecoder;
import com.ljw.channelhandler.handler.LjwrpcResponseEncoder;
import com.ljw.channelhandler.handler.MethodCallHandler;
import com.ljw.config.Configuration;
import com.ljw.core.HeartbeatDetector;
import com.ljw.discovery.Registry;
import com.ljw.discovery.RegistryConfig;
import com.ljw.loadbalancer.LoadBalancer;
import com.ljw.transport.LjwrpcRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class LjwrpcBootstrap {

    // LjwrpcBootstrap是一个单例，我们希望每个应用程序只有一个实
    private static LjwrpcBootstrap ljwrpcBootstrap = new LjwrpcBootstrap();

    // 全局的配置中心
    private Configuration configuration;

    // 保存request对象，可以在当前线程中随时获取
    public static final ThreadLocal<LjwrpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    // 注册中心
    private Registry registry;

    // 连接的缓存，如果使用InetSocketAddress这样的类作key，一定要看它是否重写了euqals和toString方法
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    public final static TreeMap<Long, Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();

    // 维护已经发布且暴露的服务列表，key -> interface 的全限定名 value-》ServiceConfig
    public final static Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    // 定义对外全局挂起的 completableFuture
    public final static Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new HashMap<>(128);

    // 维护一个zookeeper实例
    //private ZooKeeper zooKeeper;

    private LjwrpcBootstrap() {
        // 构造启动引导程序时，需要做一些什么初始化的事
        configuration = new Configuration();
    }

    public static LjwrpcBootstrap getInstance() {
        return ljwrpcBootstrap;
    }

    /**
     * 用来定义当前应用的名字
     * @param appName 应用的名字
     * @return this 当前实例
     */
    public LjwrpcBootstrap application(String appName) {
        configuration.setAppName(appName);
        return this;
    }

    /**
     * 用来配置一个注册中心
     * @param registryConfig 注册中心
     * @return this 当前实例
     */
    public LjwrpcBootstrap registry(RegistryConfig registryConfig) {
        // 这里维护一个zookeeper实例，但是如果这样写就会将zookeeper和当前工程耦合
        // 我们其实更希望以后可以扩展更多种不同的实现

        // 尝试使用 registryConfig 获取一个注册中心，有点工厂设计模式的意思
        configuration.setRegistryConfig(registryConfig);
        return this;
    }

    /**
     * 用来配置负载均衡策略
     * @param loadBalancer 注册中心
     * @return this 当前实例
     */
    public LjwrpcBootstrap loadBalancer(LoadBalancer loadBalancer) {
        configuration.setLoadBalancer(loadBalancer);
        return this;
    }

    /**
     * --------------------------------------服务提供方的相关api------------------------------------------
     */

    /**
     * 服务发布，将接口=》实现 注册到服务中心
     * @param service 封装的需要发布的服务
     * @return this 当前实例
     */
    public LjwrpcBootstrap publish(ServiceConfig<?> service) {
        // 我们抽象了注册中心的概念，使用注册中心的一个实现完成注册
        // 有人会想，此时此刻难道不是强耦合了吗？
        configuration.getRegistryConfig().getRegistry().registry(service);
        SERVERS_LIST.put(service.getInterface().getName(), service);
        return this;
    }

    /**
     * 批量发布服务
     * @param services 封装的需要发布的服务的集合
     * @return this 当前实例
     */
    public LjwrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service : services) {
            this.publish(service);
        }
        return this;
    }

    /**
     * 启动netty服务
     */
    public void start() {
        // 1. 创建eventloop，老板只负责处理请求，会将请求分发至worker
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(5);
        try {
            // 2. 需要一个服务器引导程序
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 3. 配置服务器
            serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 核心，我们需要添加很多入站和出站的handler
                            socketChannel.pipeline().addLast(new LoggingHandler())
                                    .addLast(new LjwrpcRequestDecoder())
                                    // 根据请求进行方法调用
                                    .addLast(new MethodCallHandler())
                                    .addLast(new LjwrpcResponseEncoder());
                        }
                    });
            // 4. 绑定端口
            ChannelFuture channelFuture = serverBootstrap.bind(configuration.getPort()).sync();

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * --------------------------------------服务调用方的相关api------------------------------------------
     */

    public LjwrpcBootstrap reference(ReferenceConfig<?> reference) {

        // 开启对这个服务的心跳检测
        HeartbeatDetector.detectHeartbeat(reference.getInterfaceRef().getName());

        // 在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1.reference需要一个注册中心
        reference.setRegistry(configuration.getRegistryConfig().getRegistry());
        return this;
    }

    /**
     * 配置序列化的方式
     * @param serializeType 序列化的方式
     * @return
     */
    public LjwrpcBootstrap serialize(String serializeType) {
        configuration.setSerializeType(serializeType);
        if (log.isDebugEnabled()){
            log.debug("我们配置了使用的序列化的方式为【{}】", serializeType);
        }
        return this;
    }

    public LjwrpcBootstrap compress(String compressType) {
        configuration.setCompressType(compressType);
        if (log.isDebugEnabled()){
            log.debug("我们配置了使用的压缩算法为为【{}】", compressType);
        }
        return this;
    }

    /**
     * 扫描包，进行批量注册
     * @param packageName 包名
     * @return this本身
     */
    public LjwrpcBootstrap scan(String packageName) {
        // 1、需要通过packageName获取其下的所有的类的权限定名称
        List<String> classNames = getAllClassNames(packageName);

        // 2、通过反射获取他的接口，构建具体实现
        List<Class<?>> classes = classNames.stream()
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(clazz -> clazz.getAnnotation(LjwrpcApi.class) != null)
                .collect(Collectors.toList());

        for (Class<?> clazz : classes) {
            // 获取他的接口
            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance = null;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            for (Class<?> anInterface : interfaces) {
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(anInterface);
                serviceConfig.setRef(instance);

                if (log.isDebugEnabled()){
                    log.debug("-----> 已经通过包扫描，将服务【{}】发布.", anInterface);
                }

                // 3、发布
                publish(serviceConfig);
            }

        }
        return this;
    }

    private List<String> getAllClassNames(String packageName) {
        // 1、通过传入的packageName获得决定路径 /Users/gawen/java/.../classes/com/ljw
        String basePath = packageName.replaceAll("\\.", "/");
        URL url = ClassLoader.getSystemClassLoader().getResource(basePath);
        if (url == null) {
            throw new RuntimeException("包扫描时发现路径不存在.");
        }
        String absolutePath = url.getPath();
        List<String> classNames = new ArrayList<>();
        classNames = recursionFile(absolutePath, classNames, basePath);

        return classNames;
    }

    private List<String> recursionFile(String absolutePath, List<String> classNames, String basePath) {
        // 获取文件
        File file = new File(absolutePath);
        // 判断文件是否是文件夹
        if (file.isDirectory()){
            // 找到文件夹的所有名称
            File[] children = file.listFiles(pathname -> pathname.isDirectory() || pathname.getPath().contains(".class"));
            if (children == null || children.length == 0) {
                return classNames;
            }
            for (File child : children) {
                if (child.isDirectory()){
                    // 递归调用
                    recursionFile(child.getAbsolutePath(), classNames, basePath);
                } else {
                    // 文件 -> 类的权限定名称
                    String className = getClassNameByAbsolutePath(child.getAbsolutePath(), basePath);
                    classNames.add(className);
                }
            }
        } else {
            // 文件 -> 类的权限定名称
            String className = getClassNameByAbsolutePath(absolutePath, basePath);
            classNames.add(className);
        }


        return classNames;
    }

    private String getClassNameByAbsolutePath(String absolutePath, String basePath) {
        // /Users/gawen/java/IdeaProjects/ljwrpc/ljwrpc-framework/ljwrpc-core/target/classes/com/ljw/channelhandler/handler/LjwrpcRequestDecoder.class
        // com/ljw/channelhandler/handler/LjwrpcRequestDecoder.class --> com.ljw.channelhandler.handler.LjwrpcRequestDecoder
        String fileName = absolutePath.substring(absolutePath.indexOf(basePath))
                .replaceAll("/", ".");
        fileName = fileName.substring(0, fileName.indexOf(".class"));

        return fileName;
    }

    public static void main(String[] args) {
        List<String> allClassNames = LjwrpcBootstrap.getInstance().getAllClassNames("com.ljw");
        System.out.println(allClassNames);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
