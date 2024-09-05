package com.ljw.channelhandler.handler;

import com.ljw.compress.Compressor;
import com.ljw.compress.CompressorFactory;
import com.ljw.enumeration.RequestType;
import com.ljw.serialize.Serializer;
import com.ljw.serialize.SerializerFactory;
import com.ljw.transport.LjwrpcRequest;
import com.ljw.transport.MessageFormatConstant;
import com.ljw.transport.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

/**
 * *自定义协议编码器
 * * <p>
 * * <pre>
 *  *   0    1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18   19   20   21   22
 *  *   +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
 *  *   |    magic          |ver |head  len|    full length    |code  ser|comp|              RequestId                |
 *  *   +-----+-----+-------+----+----+----+----+-----------+----- ---+--------+----+----+----+----+----+----+---+---+
 *  *   |                                                                                                             |
 *  *   |                                         body                                                                |
 *  *   |                                                                                                             |
 *  *   +--------------------------------------------------------------------------------------------------------+---+
 *  * </pre>
 * *
 * * 4B magic(魔术值，用来判断报文是否是本Ljwrpc的报文)  ---> Ljwrpc.getBytes()
 * * 1B version(版本) ---> 1
 * * 2B header length(首部的长度)
 * * 4B full length(报文总长度)
 * * 1B serialize
 * * 1B compress
 * * 1B requestType
 * * 8B requestId
 * * body
 *
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class LjwrpcRequestDecoder extends LengthFieldBasedFrameDecoder {

    public LjwrpcRequestDecoder() {
        super(
                // 找到当前报文的总长度，截取报文，截取出来的报文可以去进行解析
                // 最大帧的长度，超过这个maxFrameLength值会直接丢弃
                MessageFormatConstant.MAX_FRAME_LENGTH,
                // 长度的字段的偏移量
                MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH,
                // 长度的字段的长度
                MessageFormatConstant.FULL_FIELD_LENGTH,
                // 负载的适配长度
                -(MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH + MessageFormatConstant.FULL_FIELD_LENGTH),
                0
        );
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

        Thread.sleep(new Random().nextInt(50));

        Object decode = super.decode(ctx, in);
        if (decode instanceof ByteBuf byteBuf) {// 直接将decode强转成了ByteBuf
            return decodeFrame(byteBuf);
        }
        return null;
    }

    private Object decodeFrame(ByteBuf byteBuf) {
        // 1.解析魔数
        byte[] magic = new byte[MessageFormatConstant.MAGIC.length];
        byteBuf.readBytes(magic);
        // 检测魔数是否匹配
        for (int i = 0; i < magic.length; i++) {
            if (magic[i] != MessageFormatConstant.MAGIC[i]) {
                throw new RuntimeException("获得的请求不合法。");
            }
        }

        // 2.解析版本号
        byte version = byteBuf.readByte();
        if (version > MessageFormatConstant.VERSION) {
            throw new RuntimeException("获得的请求版本不被支持");
        }

        // 3.解析头部的长度
        short headerLength = byteBuf.readShort();

        // 4.解析总长度
        int fulllength = byteBuf.readInt();

        // 5.解析请求类型, TODO 判断是不是心跳检测
        byte requestType = byteBuf.readByte();

        // 6.解析序列化类型
        byte serializeType = byteBuf.readByte();

        // 7.解析压缩类型
        byte compressType = byteBuf.readByte();

        // 8.解析请求id
        Long requestId = byteBuf.readLong();

        // 9.时间戳
        long timeStamp = byteBuf.readLong();

        // 我们需要封装
        LjwrpcRequest ljwrpcRequest = new LjwrpcRequest();
        ljwrpcRequest.setRequestType(requestType);
        ljwrpcRequest.setCompressType(compressType);
        ljwrpcRequest.setSerializeType(serializeType);
        ljwrpcRequest.setRequestId(requestId);
        ljwrpcRequest.setTimeStamp(timeStamp);

        // 心跳请求没有负载，此处可以判断并直接返回
        if (requestType == RequestType.HEART_BEAT.getId()) {
            return ljwrpcRequest;
        }

        int payloadLength = fulllength - headerLength;
        byte[] payload = new byte[payloadLength];
        byteBuf.readBytes(payload);

        //有了字节数组之后就可以解压缩反序列化
        // 1、解压缩
        if (payload != null && payload.length != 0) {
            Compressor compressor = CompressorFactory.getCompressor(compressType).getCompressor();
            payload = compressor.decompress(payload);

            // 2、反序列化
            Serializer serializer = SerializerFactory.getSerializer(serializeType).getSerializer();
            RequestPayload requestPayload = serializer.deserialize(payload, RequestPayload.class);
            ljwrpcRequest.setRequestPayload(requestPayload);
        }

        if (log.isDebugEnabled()) {
            log.debug("请求【{}】已经在服务端完成解码工作。", ljwrpcRequest.getRequestId());
        }

        return ljwrpcRequest;
    }
}