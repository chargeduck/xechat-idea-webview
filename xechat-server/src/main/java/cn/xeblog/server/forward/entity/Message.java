package cn.xeblog.server.forward.entity;

import cn.xeblog.commons.entity.User;
import cn.xeblog.server.forward.enums.MessageType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

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

}
