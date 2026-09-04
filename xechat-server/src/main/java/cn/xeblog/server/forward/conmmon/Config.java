package cn.xeblog.server.forward.conmmon;

import cn.xeblog.server.forward.codec.MessageSerializer;

/**
 * 序列化方式配置（server 侧固定 JSON，与 hub 运行配置 application.yml serializer.algorithm=JSON 对齐）
 * <p>
 * 注意：包名沿用老 xe-forwarding-server 拼写 conmmon，保持跨项目兼容。
 *
 * @author eleven
 * @date 2024/12/16 10:49
 */
public class Config {
    public static MessageSerializer.Algorithm getSerializerAlgorithm() {
        return MessageSerializer.Algorithm.JSON;
    }
}
