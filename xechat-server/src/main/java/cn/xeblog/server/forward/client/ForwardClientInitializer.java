package cn.xeblog.server.forward.client;

import cn.xeblog.server.forward.codec.SharedMessageCodec;
import cn.xeblog.server.forward.handler.ProtocolFrameDecoder;
import cn.xeblog.server.forward.utils.MessageBuilder;
import cn.xeblog.server.cache.ForwardCache;
import cn.xeblog.server.forward.utils.XeServerUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author eleven
 * @date 2024/12/16 10:03
 */
@Slf4j
public class ForwardClientInitializer extends ChannelInitializer<NioSocketChannel> {
    @Override
    protected void initChannel(NioSocketChannel channel) throws Exception {
        ProtocolFrameDecoder protocolFrameDecoder = new ProtocolFrameDecoder();
        channel.pipeline()
                .addLast(protocolFrameDecoder)
                .addLast(new SharedMessageCodec())
                .addLast(new IdleStateHandler(30, 30, 30))
                .addLast(new ChannelDuplexHandler() {
                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        IdleStateEvent event = (IdleStateEvent) evt;
                        if (event.state() == IdleState.WRITER_IDLE) {
                            log.info("发送心跳数据包");
                            ctx.writeAndFlush(MessageBuilder.heartbeat(ForwardCache.channelShortId,
                                    ForwardCache.seq,
                                    ForwardCache.serverName,
                                    XeServerUtils.getServerInfoJsonStr()));
                        }
                    }
                })
                .addLast("forwardClientHandler", new ForwardClientChannelAdapter());
    }
}
