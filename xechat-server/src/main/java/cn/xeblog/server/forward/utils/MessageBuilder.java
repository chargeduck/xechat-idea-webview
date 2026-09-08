package cn.xeblog.server.forward.utils;

import cn.xeblog.commons.entity.User;
import cn.xeblog.server.forward.entity.Message;
import cn.xeblog.server.forward.enums.MessageType;

import java.util.List;

/**
 * 帧构造器（server 侧上行上报所需子集）
 * <p>
 * 与 xechat-forward-match-lobby 中 MessageBuilder 的 heartbeat/userMessage/userOfflineMessage
 * 语义保持一致；不依赖 hub 独有的 ClientCache 等类。
 *
 * @author eleven
 * @date 2024/12/16 16:11
 */
public class MessageBuilder {

    /**
     * 心跳
     */
    public static Message heartbeat(String id, Integer sequenceId, String serverName, Object data) {
        return new Message()
                .setMessageType(MessageType.HEARTBEAT)
                .setId(id)
                .setSequenceId(sequenceId)
                .setServerName(serverName)
                .setData(data);
    }

    /**
     * 用户消息（data 为 Request）
     */
    public static Message userMessage(User user, Object msg) {
        return new Message()
                .setMessageType(MessageType.MESSAGE)
                .setUser(user)
                .setData(msg);
    }

    /**
     * 用户下线上报（data 为 username）
     */
    public static Message userOfflineMessage(String serverName, String username) {
        return new Message()
                .setMessageType(MessageType.USER_OFFLINE)
                .setServerName(serverName)
                .setData(username);
    }

    /**
     * 用户下线上报（升级：user 放 user 字段，供 hub 按 uuid 精确剔除路由表）
     */
    public static Message userOfflineMessage(String serverName, User user) {
        return new Message()
                .setMessageType(MessageType.USER_OFFLINE)
                .setServerName(serverName)
                .setUser(user);
    }

    /**
     * 用户上线增量上报（user 放 user 字段，上行本塘原始用户对象）
     */
    public static Message userOnlineMessage(String serverName, User user) {
        return new Message()
                .setMessageType(MessageType.USER_ONLINE)
                .setServerName(serverName)
                .setUser(user);
    }

    /**
     * 全量在线快照上报（server 注册/重连成功后上行本塘全部在线用户，列表走 users 字段）
     */
    public static Message onlineUsersMessage(String serverName, List<User> users) {
        return new Message()
                .setMessageType(MessageType.ONLINE_USERS)
                .setServerName(serverName)
                .setUsers(users);
    }
}
