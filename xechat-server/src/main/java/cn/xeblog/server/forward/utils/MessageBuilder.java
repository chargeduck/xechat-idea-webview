package cn.xeblog.server.forward.utils;

import cn.xeblog.commons.entity.User;
import cn.xeblog.server.forward.entity.Message;
import cn.xeblog.server.forward.enums.MessageType;

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
}
