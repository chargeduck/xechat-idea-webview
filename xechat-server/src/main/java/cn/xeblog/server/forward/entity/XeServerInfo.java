package cn.xeblog.server.forward.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 鱼塘接入信息（心跳 data 上报，供 hub 维护鱼塘列表/连接指引）
 * <p>
 * 注意：协议共享类，与 xechat-forward-match-lobby 中同名类保持同步。
 *
 * @author eleven
 * @date 2024/12/18 11:26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class XeServerInfo {
    private String name;

    private String domain;

    private String host;

    private Integer port;

    private Boolean enabledWS;

    private Boolean editable;
}
