package cn.xeblog.server.forward.client;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.xeblog.commons.entity.LoginDTO;
import cn.xeblog.commons.entity.Request;
import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.entity.UserMsgDTO;
import cn.xeblog.commons.enums.Action;
import cn.xeblog.server.forward.entity.Message;
import cn.xeblog.server.forward.entity.XeServerInfo;
import cn.xeblog.server.forward.enums.MessageType;
import cn.xeblog.server.forward.utils.MessageBuilder;
import cn.xeblog.server.action.ChannelAction;
import cn.xeblog.server.builder.ResponseBuilder;
import cn.xeblog.server.cache.ForwardCache;
import cn.xeblog.server.forward.utils.XeServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static cn.xeblog.commons.enums.MessageType.USER;

/**
 * hub 下行消息处理（注册身份/转发消息/用户下线广播/服务器状态/鱼塘列表等）
 *
 * @author eleven
 * @date 2024/12/16 10:08
 */
@Slf4j
public class ForwardClientChannelAdapter extends SimpleChannelInboundHandler<Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) throws Exception {
        MessageType messageType = message.getMessageType();
        String serverName = message.getServerName();
        User user = message.getUser();
        if (messageType == MessageType.SEQUENCE_ID) {
            ForwardCache.seq = message.getSequenceId();
            ForwardCache.channelShortId = message.getId();
            // hub 分配的 index 名仅作兜底：配置名或探测名已就绪时不覆盖
            if (StrUtil.isBlank(XeServerUtils.resolveServerName())) {
                ForwardCache.serverName = serverName;
            }
            String serverOnlineMsg = StrUtil.format("{} 上线了", serverName);
            ChannelAction.send(ResponseBuilder.system(serverOnlineMsg));
        }
        if (messageType == MessageType.MESSAGE) {
            // 不是本服务的消息才转发
            if (!StrUtil.equalsIgnoreCase(message.getId(), ForwardCache.channelShortId)) {
                if (!(message.getData() instanceof Map)) {
                    return;
                }
                Request request = BeanUtil.mapToBean((Map<?, ?>) message.getData(), Request.class, false);
                if (request == null || request.getAction() == null) {
                    return;
                }
                Action action = request.getAction();
                if (action == Action.CHAT) {
                    if (user == null) {
                        return;
                    }
                    user.setShortRegion(StrUtil.format("{} -> {}", serverName, user.getShortRegion()));
                    UserMsgDTO userMsgDTO = BeanUtil.mapToBean((Map<?, ?>) request.getBody(), UserMsgDTO.class, false);
                    ChannelAction.send(ResponseBuilder.build(user, userMsgDTO, USER));
                } else if (action == Action.LOGIN) {
                    LoginDTO loginDTO = BeanUtil.mapToBean((Map<?, ?>) request.getBody(), LoginDTO.class, false);
                    String otherServerLoginMsg = StrUtil.format("[{}] 用户 [{}] 上线了", serverName, loginDTO.getUsername());
                    ChannelAction.send(ResponseBuilder.system(otherServerLoginMsg));
                }
            }
        }

        if (messageType == MessageType.USER_OFFLINE) {
            // hub 原样转发 USER_OFFLINE，data 为下线的用户名
            String username = message.getData() == null ? "未知" : String.valueOf(message.getData());
            ChannelAction.send(ResponseBuilder.system(StrUtil.format("【{}】 用户 [{}] 下线了", serverName, username)));
        }

        if (messageType == MessageType.SERVER_ONLINE) {
            ChannelAction.send(ResponseBuilder.system(StrUtil.format("【{}】 已连接至转发服务器", serverName)));
        }
        if (messageType == MessageType.SERVER_OFFLINE) {
            ChannelAction.send(ResponseBuilder.system(StrUtil.format("【{}】 已从转发服务器断开连接", serverName)));
        }
        if (messageType == MessageType.FORWARD_SERVER_LIST) {
            Map<String, Map<?, ?>> srcMap = (Map<String, Map<?, ?>>) message.getData();
            Map<String, XeServerInfo> forwardServerList = new HashMap<>();
            srcMap.forEach((k, v) -> forwardServerList.put(k, BeanUtil.mapToBean(v, XeServerInfo.class, false)));
            StringBuilder sb = new StringBuilder("当前在线服务器 \n");
            sb.append(StrUtil.padAfter("index", 8, ' ')).append(StrUtil.padAfter("name", 16, ' '))
                    .append(StrUtil.padAfter("host", 24, ' ')).append(StrUtil.padAfter("port", 8, ' '))
                    .append(StrUtil.padAfter("enableWs", 10, ' ')).append("editable").append("\n");
            forwardServerList.forEach((k, v) -> sb
                    .append(StrUtil.padAfter(k, 8, ' '))
                    .append(StrUtil.padAfter(String.valueOf(v.getName()), 16, ' '))
                    .append(StrUtil.padAfter(Optional.ofNullable(v.getDomain()).orElse(v.getHost()), 24, ' '))
                    .append(StrUtil.padAfter(String.valueOf(v.getPort()), 8, ' '))
                    .append(StrUtil.padAfter(String.valueOf(v.getEnabledWS()), 10, ' '))
                    .append(String.valueOf(v.getEditable()))
                    .append("\n"));
            ChannelAction.send(ResponseBuilder.system(sb.toString()));
        }

        if (messageType == MessageType.ERROR_MESSAGE) {
            ChannelAction.send(ResponseBuilder.system(StrUtil.format("【{}】 发生错误: {}", serverName, message.getData())));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(MessageBuilder.heartbeat(null, 0, null, XeServerUtils.getServerInfoJsonStr()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("ForwardClient channel 断开连接");
        super.channelInactive(ctx);
    }
}
