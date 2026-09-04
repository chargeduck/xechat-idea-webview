package cn.xeblog.server.forward.codec;

import cn.xeblog.server.forward.conmmon.Config;
import cn.xeblog.server.forward.entity.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 帧编解码器（协议与 xechat-forward-match-lobby / xe-forwarding-server 完全兼容）
 * <p>
 * 协议共享类，与 xechat-forward-match-lobby 中同名类保持同步。
 *
 * @author eleven
 * @date 2024/12/16 8:00
 */
@Slf4j
@ChannelHandler.Sharable
public class SharedMessageCodec extends MessageToMessageCodec<ByteBuf, Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> outList) throws Exception {
        ByteBuf out = ctx.alloc().buffer();
        // 1. 8 字节魔数
        out.writeBytes("AreYouOk".getBytes());
        // 2. 1 字节版本
        out.writeByte(1);
        // 3. 1 字节序列化方式 0jdk 1json
        MessageSerializer.Algorithm serializerAlgorithm = Config.getSerializerAlgorithm();
        out.writeByte(serializerAlgorithm.ordinal());
        // 4. 1 字节指令类型
        out.writeByte(msg.getMessageType().ordinal());
        // 5. 4 字节请求序号
        out.writeInt(msg.getSequenceId());
        // 5.1 无意义字节（对齐到 2 的整数倍）
        out.writeByte(0xff);
        // 6. 正文长度
        byte[] bytes = serializerAlgorithm.serialize(msg);
        out.writeInt(bytes.length);
        // 7. 正文
        out.writeBytes(bytes);
        outList.add(out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 1. 读取八个字节魔数
        byte[] magicByte = new byte[8];
        in.readBytes(magicByte).toString(StandardCharsets.UTF_8);
        // 2. 读取版本
        in.readByte();
        // 3. 读取序列化方式
        byte serializerType = in.readByte();
        // 4. 读取指令类型
        in.readByte();
        // 5. 读取请求序号
        in.readInt();
        // 6. 读取无意义字节
        in.readByte();
        // 7. 读取正文长度
        int length = in.readInt();
        // 8. 读取正文
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        // 获取反序列化算法
        MessageSerializer.Algorithm algorithm = MessageSerializer.Algorithm.values()[serializerType];
        // 确定具体的消息类型
        Object message = algorithm.deserialize(Message.class, bytes);
        out.add(message);
    }
}
