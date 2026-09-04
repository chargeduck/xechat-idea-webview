package cn.xeblog.server.cache;

/**
 * 转发链路状态缓存（与 hub 注册身份）
 *
 * @author eleven
 * @date 2024/12/16 13:58
 */
public class ForwardCache {
    public static volatile Integer seq;

    public static volatile String channelShortId;
    public static volatile String serverName;
}
