package com.ljw.transport.message;

import java.nio.charset.StandardCharsets;

/**
 * *自定义协议编码器
 *  * <p>
 *  * <pre>
 *  *   0    1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18   19   20   21   22
 *  *   +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
 *  *   |    magic          |ver |head  len|    full length    |code  ser|comp|              RequestId                |
 *  *   +-----+-----+-------+----+----+----+----+-----------+----- ---+--------+----+----+----+----+----+----+---+---+
 *  *   |                                                                                                             |
 *  *   |                                         body                                                                |
 *  *   |                                                                                                             |
 *  *   +--------------------------------------------------------------------------------------------------------+---+
 *  * </pre>
 * @author 刘家雯
 * @version 1.0
 */
public class MessageFormatConstant {

    public final static byte[] MAGIC = "ljwrpc".getBytes();
    public final static byte VERSION = 1;
    // 头部信息的长度
    public final static short HEADER_LENGTH = (byte)(MAGIC.length + 1 + 2 + 4 + 1 + 1 + 1 + 8);

    public final static int MAX_FRAME_LENGTH = 1024 * 1024;

    public static final int VERSION_LENGTH = 1;
    // 头部信息长度占用的偏移量
    public static final int HEADER_FIELD_LENGTH = 2;
    // 总长度占用的字节数
    public static final int FULL_FIELD_LENGTH = 4;
}
