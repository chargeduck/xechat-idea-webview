package cn.xeblog.server.forward.enums;

/**
 * 转发帧类型（协议与 xechat-forward-match-lobby 完全兼容）
 * <p>
 * 注意：协议共享枚举，与 xechat-forward-match-lobby 中同名枚举保持同步。
 *
 * @author eleven
 * @date 2024/12/16 13:21
 */
public enum MessageType {
    /**
     * 心跳
     */
    HEARTBEAT,
    /**
     * 序列号（hub 下发注册身份）
     */
    SEQUENCE_ID,
    /**
     * 服务器上线
     */
    SERVER_ONLINE,
    /**
     * 服务器列表
     */
    FORWARD_SERVER_LIST,
    /**
     * 用户下线
     */
    USER_OFFLINE,
    /**
     * 用户上线（user 字段携带用户对象）
     */
    USER_ONLINE,
    /**
     * 全量在线用户列表（快照上报 / hub 聚合 reply，users 字段携带）
     */
    ONLINE_USERS,
    /**
     * 服务器下线
     */
    SERVER_OFFLINE,
    /**
     * 错误消息
     */
    ERROR_MESSAGE,
    /**
     * 消息（上行请求/下行转发均走此类型，data 为 Request）
     */
    MESSAGE,
    ;
}
