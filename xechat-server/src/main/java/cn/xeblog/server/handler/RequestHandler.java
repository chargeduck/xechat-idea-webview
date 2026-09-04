package cn.xeblog.server.handler;

import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import cn.xeblog.commons.entity.Request;
import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.enums.Action;
import cn.xeblog.commons.enums.Protocol;
import cn.xeblog.commons.enums.UserStatus;
import cn.xeblog.server.action.handler.ActionHandler;
import cn.xeblog.server.forward.entity.Message;
import cn.xeblog.server.forward.utils.MessageBuilder;
import cn.xeblog.server.action.ChannelAction;
import cn.xeblog.server.builder.ResponseBuilder;
import cn.xeblog.server.cache.ForwardCache;
import cn.xeblog.server.cache.UserCache;
import cn.xeblog.server.factory.ActionHandlerFactory;
import cn.xeblog.server.forward.client.ForwardClient;
import cn.xeblog.server.forward.utils.XeServerUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author anlingyi
 * @date 2020/8/14
 */
public class RequestHandler {

    private final ChannelHandlerContext ctx;

    private final Request request;

    public RequestHandler(final ChannelHandlerContext ctx, final Request request) {
        this.ctx = ctx;
        this.request = request;
    }

    public void exec() {
        if (request.getAction() == null || request.getAction() == Action.HEARTBEAT) {
            return;
        }

        if (ObjectUtil.isEmpty(request.getBody())) {
            ctx.writeAndFlush(ResponseBuilder.system("Body is null!"));
            return;
        }

        // 上行请求转发给 hub（跨塘广播，来源服务不会收到自己消息）
        forwardServerRequest();

        GlobalThreadPool.execute(() -> {
            ActionHandler produce = ActionHandlerFactory.INSTANCE.produce(request.getAction());
            Object body = request.getBody();
            // 非默认协议需要转换body的数据类型
            if (request.getProtocol() != Protocol.DEFAULT) {
                try {
                    if (request.getAction() == Action.SET_STATUS) {
                        body = UserStatus.valueOf(body.toString());
                    } else {
                        body = JSONUtil.toBean(body.toString(), ClassUtil.getTypeArgument(produce.getClass()));
                    }
                } catch (Exception e) {
                    ctx.writeAndFlush(ResponseBuilder.system("消息内容解析异常!"));
                    return;
                }
            }

            produce.handle(ctx, body);
        });
    }

    /**
     * 上行请求上报转发至 hub（hub 按 Action 分流：/forwardServer 由 hub 处理，其余广播给其他 server）
     */
    private void forwardServerRequest() {
        Channel forwardChannel = ForwardClient.channel();
        if (forwardChannel == null) {
            return;
        }
        String id = ChannelAction.getId(ctx);
        User user = UserCache.get(id);
        Message message = MessageBuilder.userMessage(user, request);
        message.setSequenceId(ForwardCache.seq);
        message.setId(ForwardCache.channelShortId);
        // 补源塘名（配置/探测名，缺省回退 hub 分配的 index 名），否则跨塘展示 [null]
        message.setServerName(XeServerUtils.getDisplayServerName());
        forwardChannel.writeAndFlush(message);
    }

}
