package cn.xeblog.server.forward.handler;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 帧解码器（单帧上限 1MB，与 xechat-forward-match-lobby 保持同步）
 * <p>
 * 协议共享类，与 xechat-forward-match-lobby 中同名类保持同步。
 *
 * @author eleven
 * @date 2024/10/31 14:47
 */
public class ProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {

    public ProtocolFrameDecoder() {
        this(1024 * 1024, 16, 4, 0, 0);
    }

    public ProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
}
