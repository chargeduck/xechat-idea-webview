package cn.xeblog.server.forward.entity;

import cn.xeblog.commons.entity.User;
import cn.xeblog.server.forward.enums.MessageType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 转发/聚合帧实体（协议与 xechat-forward-match-lobby 完全兼容）
 * <p>
 * 注意：本类为跨进程协议共享类，与 xechat-forward-match-lobby 中同名类保持同步，
 * 双方必须使用完全一致的字段与序列化协议，禁止单侧随意修改。
 *
 * @author eleven
 * @date 2024/12/16 8:01
 */
@Data
@Accessors(chain = true)
public class Message implements Serializable {

    /**
     * 来源通道短id（server 注册后由 hub 分配）
     */
    private String id;

    private MessageType messageType;

    private Integer sequenceId;

    /**
     * 来源鱼塘名（可读名称）
     */
    private String serverName;

    private User user;

    private Object data;

    /**
     * 在线用户聚合列表（ONLINE_USERS 快照上报/聚合 reply、SERVER_OFFLINE 携带的断塘用户列表）
     * <p>
     * 说明：两侧固定 JSON 序列化，data(Object) 内嵌 User/List 会被 Gson 还原为 Map，
     * 因此单用户走 {@link #user} 字段、列表走本字段，data 仅保留字符串/无类型载荷。
     */
    private List<User> users;

}
