package cn.xeblog.server.forward.utils;

import cn.hutool.core.util.StrUtil;
import cn.xeblog.server.cache.ForwardCache;
import cn.xeblog.server.config.ServerConfig;
import cn.xeblog.server.forward.entity.XeServerInfo;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务器注册信息工具：接入名仅取 yml 配置（forward.serverName / -sn）
 * <p>
 * server 只负责把配置传上来；未配置时不探测、不上报名字，
 * 由 hub(forward) 按连接地址比对 manager 服务器列表探测登记名，
 * 仍未命中则保持 hub 分配的接入 index（N号鱼塘）兜底展示。
 *
 * @author eleven
 * @date 2024/12/18 11:28
 */
@Slf4j
public final class XeServerUtils {

    private static final Gson GSON = new Gson();

    private XeServerUtils() {
    }

    /**
     * 心跳上报 JSON：name 取 yml 配置名；未配置则为 null（不覆盖 hub 默认名，也不阻断 hub 探测）
     */
    public static String getServerInfoJsonStr() {
        ServerConfig config = ServerConfig.getConfig();
        String name = resolveServerName();
        return GSON.toJson(new XeServerInfo()
                .setName(StrUtil.isBlank(name) ? null : name)
                .setPort(config.getPort())
                .setEditable(true)
                .setEnabledWS(config.getEnableWS())
        );
    }

    /**
     * 接入名解析：yml 配置名，未配置返回 null（交由 hub 探测/兜底）
     */
    public static String resolveServerName() {
        return ServerConfig.getConfig().getServerName();
    }

    /**
     * 展示名：yml 配置名优先，空则回退 hub 分配的 index 名（兜底）
     */
    public static String getDisplayServerName() {
        String name = resolveServerName();
        if (StrUtil.isBlank(name)) {
            name = ForwardCache.serverName;
        }
        return name;
    }
}
