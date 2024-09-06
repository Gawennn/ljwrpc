package com.ljw.channelhandler.handler;

import com.ljw.compress.Compressor;
import com.ljw.compress.CompressorFactory;
import com.ljw.serialize.Serializer;
import com.ljw.serialize.SerializerFactory;
import com.ljw.transport.LjwrpcRequest;
import com.ljw.transport.MessageFormatConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 *自定义协议编码器
 * <p>
 * <pre>
 *   0    1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18   19   20   21   22
 *   +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
 *   |    magic          |ver |head  len|    full length    |code  ser|comp|              RequestId                |
 *   +-----+-----+-------+----+----+----+----+-----------+----- ---+--------+----+----+----+----+----+----+---+---+
 *   |                                                                                                             |
 *   |                                         body                                                                |
 *   |                                                                                                             |
 *   +--------------------------------------------------------------------------------------------------------+---+
 * </pre>
 *
 * 4B magic(魔术值，用来判断报文是否是本Ljwrpc的报文)  ---> Ljwrpc.getBytes()
 * 1B version(版本) ---> 1
 * 2B header length(首部的长度)
 * 4B full length(报文总长度)
 * 1B serialize
 * 1B compress
 * 1B requestType
 * 8B requestId
 * body
 *
 * 出站时，第一个经过的处理器
 *
 * @author 刘家雯
 * @version 1.0
 */
@Slf4j
public class LjwrpcRequestEncoder extends MessageToByteEncoder<LjwrpcRequest> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, LjwrpcRequest ljwrpcRequest, ByteBuf byteBuf) throws Exception {
        // 4个字节的魔数值
        byteBuf.writeBytes(MessageFormatConstant.MAGIC);
        // 1个字节的版本号
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        // 2个字节的头部长度
        byteBuf.writeByte(MessageFormatConstant.HEADER_LENGTH);
        // 总长度不清楚，不知道body的长度 writerIndex(写指针)
        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH); // 当前写指针位置 + 总长度的位置
        // 3个类型
        byteBuf.writeByte(ljwrpcRequest.getRequestType());
        byteBuf.writeByte(ljwrpcRequest.getSerializeType());
        byteBuf.writeByte(ljwrpcRequest.getCompressType());
        // 8个字节的请求id
        byteBuf.writeLong(ljwrpcRequest.getRequestId());
        byteBuf.writeLong(ljwrpcRequest.getTimeStamp());

        // 如果是心跳请求，就不处理请求体
//        if (ljwrpcRequest.getRequestType() == RequestType.HEART_BEAT.getId()) {
//            // 处理以下总长度，其实总长度 = heder长度
//            int writerIndex = byteBuf.writerIndex();
//            byteBuf.writerIndex(MessageFormatConstant.MAGIC.length
//                    + MessageFormatConstant.VERSION_LENGTH
//                    + MessageFormatConstant.HEADER_FIELD_LENGTH);
//            byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH);
//
//            return;
//        }

        // 写入请求体（requestPayload）
        // 1.根据配置的序列化方式进行序列化
        // 怎么实现 1、工具类 耦合性高 如果以后想替换序列化方式很难
        byte[] body = null;
        if (ljwrpcRequest.getRequestPayload() != null) {
            Serializer serializer = SerializerFactory.getSerializer(ljwrpcRequest.getSerializeType()).getImpl();
            body = serializer.serialize(ljwrpcRequest.getRequestPayload());

        // 2.根据配置的压缩方式进行压缩
            Compressor compressor = CompressorFactory.getCompressor(ljwrpcRequest.getCompressType()).getImpl();
            body = compressor.compress(body);
        }

        if (body != null) {
            byteBuf.writeBytes(body);
        }
        int bodyLength = body == null ? 0 : body.length;

        // 重新处理报文的总长度
        // 先保存当前的写指针的位置
        int writerIndex = byteBuf.writerIndex();
        // 将写指针的位置移动到总长度的位置上
        byteBuf.writerIndex(MessageFormatConstant.MAGIC.length
                + MessageFormatConstant.VERSION_LENGTH
                + MessageFormatConstant.HEADER_FIELD_LENGTH);
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + bodyLength);

        //将写指针归位
        byteBuf.writerIndex(writerIndex);

        if (log.isDebugEnabled()) {
            log.debug("请求【{}】已经完成报文的编码", ljwrpcRequest.getRequestId());
        }
    }

}
