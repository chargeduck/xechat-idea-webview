package cn.xeblog.server.forward.client;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.xeblog.commons.entity.LoginDTO;
import cn.xeblog.commons.entity.Request;
import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.entity.UserMsgDTO;
import cn.xeblog.commons.entity.UserStateMsgDTO;
import cn.xeblog.commons.enums.Action;
import cn.xeblog.server.forward.entity.Message;
import cn.xeblog.server.forward.entity.XeServerInfo;
import cn.xeblog.server.forward.enums.MessageType;
import cn.xeblog.server.forward.utils.MessageBuilder;
import cn.xeblog.server.action.ChannelAction;
import cn.xeblog.server.builder.ResponseBuilder;
import cn.xeblog.server.cache.ForwardCache;
import cn.xeblog.server.cache.ForwardUserCache;
import cn.xeblog.server.cache.UserCache;
import cn.xeblog.server.forward.utils.XeServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
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
            // 注册/重连成功：向 hub 上报本塘全量在线快照，触发 hub 侧 diff 与外塘聚合 reply
            List<User> localSnapshot = UserCache.listUser();
            log.info("[塘转] 收到 SEQUENCE_ID 注册成功: seq={}, channelId={}, 本塘在线 {} 人, 上行全量快照 ONLINE_USERS -> hub",
                    message.getSequenceId(), message.getId(), localSnapshot.size());
            channelHandlerContext.writeAndFlush(MessageBuilder.onlineUsersMessage(XeServerUtils.getDisplayServerName(), localSnapshot));
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

        if (messageType == MessageType.USER_ONLINE) {
            // hub 广播其他鱼塘用户上线：入外塘视图并广播上线状态（外塘用户绝不写本塘 UserCache）
            User extUser = message.getUser();
            if (extUser == null) {
                log.warn("[塘转] 下行 USER_ONLINE 丢弃: 帧内 user 为空, server={}", serverName);
                return;
            }
            log.info("[塘转] 下行 USER_ONLINE 他塘用户上线: server={}, user={}, uuid={}",
                    serverName, extUser.getUsername(), extUser.getUuid());
            ForwardUserCache.addUser(extUser);
            ChannelAction.sendUserState(extUser, UserStateMsgDTO.State.ONLINE);
        }

        if (messageType == MessageType.USER_OFFLINE) {
            // hub 广播其他鱼塘用户下线：user 帧按 uuid 精确移除；老帧 data=username 走 username 兼容
            User offUser = message.getUser();
            if (offUser != null) {
                log.info("[塘转] 下行 USER_OFFLINE 他塘用户下线: server={}, user={}, uuid={}",
                        serverName, offUser.getUsername(), offUser.getUuid());
                ForwardUserCache.removeByUuid(offUser.getUuid());
                ChannelAction.sendUserState(offUser, UserStateMsgDTO.State.OFFLINE);
            } else if (message.getData() != null) {
                log.info("[塘转] 下行 USER_OFFLINE(老帧 username): server={}, data={}", serverName, message.getData());
                ForwardUserCache.removeByUsername(String.valueOf(message.getData()));
            }
            String username = offUser == null ? "未知" : offUser.getUsername();
            ChannelAction.send(ResponseBuilder.system(StrUtil.format("【{}】 用户 [{}] 下线了", serverName, username)));
        }

        if (messageType == MessageType.ONLINE_USERS) {
            // hub 聚合 reply：整表替换外塘视图后向本塘客户端合并广播在线列表（本塘 + 外塘）
            List<User> extUsers = message.getUsers();
            if (extUsers == null) {
                log.warn("[塘转] 下行 ONLINE_USERS 丢弃: users 字段为空");
                return;
            }
            log.info("[塘转] 下行 ONLINE_USERS hub 聚合 reply: 收到外塘 {} 人, 整表替换外塘视图", extUsers.size());
            ForwardUserCache.resetAll(extUsers);
            ChannelAction.sendOnlineUsers();
        }

        if (messageType == MessageType.SERVER_ONLINE) {
            ChannelAction.send(ResponseBuilder.system(StrUtil.format("【{}】 已连接至转发服务器", serverName)));
        }
        if (messageType == MessageType.SERVER_OFFLINE) {
            // 断塘广播可能携带该塘在线用户列表：逐条剔除外塘视图并广播下线状态
            List<User> offlineUsers = message.getUsers();
            log.info("[塘转] 下行 SERVER_OFFLINE: server={}, 携带断塘用户 {} 人", serverName,
                    offlineUsers == null ? 0 : offlineUsers.size());
            if (CollectionUtil.isNotEmpty(offlineUsers)) {
                offlineUsers.forEach(offUser -> {
                    ForwardUserCache.removeByUuid(offUser.getUuid());
                    ChannelAction.sendUserState(offUser, UserStateMsgDTO.State.OFFLINE);
                });
            }
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
        log.info("[塘转] ForwardClient 已连接 hub, 发送心跳注册, serverName={}", XeServerUtils.getDisplayServerName());
        ctx.writeAndFlush(MessageBuilder.heartbeat(null, 0, null, XeServerUtils.getServerInfoJsonStr()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("ForwardClient channel 断开连接");
        super.channelInactive(ctx);
    }
}
