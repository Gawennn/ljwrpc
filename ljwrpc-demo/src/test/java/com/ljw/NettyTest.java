package com.ljw;

import com.ljw.netty.AppClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author 刘家雯
 * @version 1.0
 */
public class NettyTest {

    @Test
    public void testByteBuf(){
        ByteBuf header = Unpooled.buffer(); // 创建一个ByteBuf单位
        ByteBuf body = Unpooled.buffer();

        // 通过逻辑组装而不是物理拷贝，实现jvm中的零拷贝
        CompositeByteBuf byteBuf = Unpooled.compositeBuffer();
        byteBuf.addComponents(header, body);
    }

    @Test
    public void testSlice(){
        byte[] byte1 = new byte[1024];
        byte[] byte2 = new byte[1024];
        // 共享byte数组的内容而不是拷贝，这也算零拷贝
        ByteBuf byteBuf = Unpooled.wrappedBuffer(byte1, byte2);

        //同样可以将一个byteBuf分割成多个，使用共享地址而非拷贝
        ByteBuf buf1 = byteBuf.slice(1, 5);
        ByteBuf buf2 = byteBuf.slice(6, 15);
    }

    @Test
    public void testMessage() throws IOException {
        ByteBuf message = Unpooled.buffer();
        message.writeBytes("ljw".getBytes(StandardCharsets.UTF_8)); // 魔术值
        message.writeByte(1); // version
        message.writeShort(125);
        message.writeInt(256);
        message.writeByte(1);
        message.writeByte(0);
        message.writeByte(2);
        message.writeLong(315553L); // id

        // 用对象流转化为字节数组
        AppClient appClient = new AppClient(); // 举个例子
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(outputStream); // 用ObjectOutputStream修饰ByteArrayOutputStream
        oos.writeObject(appClient); // 写出这个object
        byte[] bytes = outputStream.toByteArray(); // 再转化这个类变为字节到字节数组

        message.writeBytes(bytes);

        System.out.println(message);
    }
}
