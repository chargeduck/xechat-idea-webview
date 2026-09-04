package cn.xeblog.server.forward.utils;

import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.xeblog.commons.entity.OnlineServer;
import cn.xeblog.commons.util.ServerUtils;
import cn.xeblog.server.cache.ForwardCache;
import cn.xeblog.server.config.ServerConfig;
import cn.xeblog.server.forward.client.ForwardClient;
import cn.xeblog.server.forward.entity.XeServerInfo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 服务器注册信息工具：三级命名优先级
 * 1. application.yml forward.serverName（或启动参数 -sn）
 * 2. 按本服务公网 IP 比对 xechat-manager /api/server/list 登记名（后台探测缓存，1 级未配置时使用）
 * 3. 均未命中时由 hub 以接入 index（N号鱼塘）兜底展示
 *
 * @author eleven
 * @date 2024/12/18 11:28
 */
@Slf4j
public final class XeServerUtils {

    private static final Gson GSON = new Gson();

    /**
     * 公网出口 IP 回显地址（可通过 -Dxechat.publicIpUrl 覆盖）
     */
    private static final String PUBLIC_IP_ECHO_URL =
            System.getProperty("xechat.publicIpUrl", "https://ip.3322.net");

    /**
     * 网络请求超时（毫秒）
     */
    private static final int HTTP_TIMEOUT_MILLIS = 5000;

    /**
     * 探测自愈最小间隔（毫秒）
     */
    private static final long DETECT_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private static volatile String detectedServerName;

    private static volatile long lastDetectTime;

    private XeServerUtils() {
    }

    /**
     * 心跳上报 JSON：name 优先配置名，其次探测缓存；均无则用 hub 分配的 index 名兜底（不为空，避免覆盖掉默认展示名）
     */
    public static String getServerInfoJsonStr() {
        ServerConfig config = ServerConfig.getConfig();
        String name = resolveServerName();
        if (StrUtil.isBlank(name)) {
            name = ForwardCache.serverName;
            if (StrUtil.isBlank(name)) {
                maybeRefreshDetectAsync();
            }
        }
        return GSON.toJson(new XeServerInfo()
                .setName(name)
                .setPort(config.getPort())
                .setEditable(true)
                .setEnabledWS(config.getEnableWS())
        );
    }

    /**
     * 接入名解析（1/2 级；均未命中返回 null）
     */
    public static String resolveServerName() {
        String configured = ServerConfig.getConfig().getServerName();
        if (StrUtil.isNotBlank(configured)) {
            return configured;
        }
        return detectedServerName;
    }

    /**
     * 展示名：配置/探测名优先，空则回退 hub 分配的 index 名（兜底）
     */
    public static String getDisplayServerName() {
        String name = resolveServerName();
        if (StrUtil.isBlank(name)) {
            name = ForwardCache.serverName;
        }
        return name;
    }

    /**
     * 触发一次异步远程探测（不阻塞调用方）；探测成功后如已接入 hub 会补发心跳更新名单
     */
    public static void refreshDetectAsync() {
        GlobalThreadPool.execute(() -> {
            try {
                String name = detectServerNameByPublicIp();
                detectedServerName = name;
                lastDetectTime = System.currentTimeMillis();
                if (StrUtil.isNotBlank(name)) {
                    log.info("已按公网 IP 探测到本服务接入名: {}", name);
                    ForwardClient.notifyServerInfoChanged();
                } else {
                    log.info("未按公网 IP 匹配到登记接入名，将使用 hub 接入 index 兜底展示");
                }
            } catch (Exception e) {
                log.warn("探测本服务接入名失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 距上次探测超过间隔且仍未探测到名字时，异步再探一次（防漏配后永远停留在 index 兜底）
     */
    private static synchronized void maybeRefreshDetectAsync() {
        if (System.currentTimeMillis() - lastDetectTime < DETECT_INTERVAL_MILLIS) {
            return;
        }
        lastDetectTime = System.currentTimeMillis();
        refreshDetectAsync();
    }

    /**
     * 按公网 IP 比对管理后台服务器列表，返回登记名；匹配同时参考本服务端口（同机多实例可区分）
     */
    private static String detectServerNameByPublicIp() {
        String publicIp = resolvePublicIp();
        if (StrUtil.isBlank(publicIp)) {
            return null;
        }
        ServerConfig config = ServerConfig.getConfig();
        for (OnlineServer server : fetchServerList()) {
            if (StrUtil.isBlank(server.getIp())) {
                continue;
            }
            boolean ipMatched = ipEquals(server.getIp(), publicIp);
            boolean portMatched = server.getPort() == null || config.getPort() == null
                    || server.getPort().equals(config.getPort());
            if (ipMatched && portMatched) {
                return server.getName();
            }
        }
        return null;
    }

    /**
     * 解析本服务公网地址：优先显式配置 server.publicIp，否则请求公网出口 IP 回显服务
     */
    private static String resolvePublicIp() {
        String configured = ServerConfig.getConfig().getPublicIp();
        if (StrUtil.isNotBlank(configured)) {
            return configured.trim();
        }
        try {
            String body = HttpRequest.get(PUBLIC_IP_ECHO_URL).timeout(HTTP_TIMEOUT_MILLIS).execute().body();
            return StrUtil.isBlank(body) ? null : body.trim();
        } catch (Exception e) {
            log.warn("获取公网出口 IP 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 拉取管理后台启用中的服务器列表（兼容裸数组与 Result 包装）
     */
    private static List<OnlineServer> fetchServerList() {
        try {
            String resp = HttpRequest.get(ServerUtils.getFetchServerListUrl())
                    .timeout(HTTP_TIMEOUT_MILLIS).execute().body();
            if (StrUtil.isBlank(resp)) {
                return Collections.emptyList();
            }
            JsonElement element = JsonParser.parseString(resp);
            JsonArray array = null;
            if (element.isJsonArray()) {
                array = element.getAsJsonArray();
            } else if (element.isJsonObject()) {
                JsonElement data = element.getAsJsonObject().get("data");
                if (data != null && data.isJsonArray()) {
                    array = data.getAsJsonArray();
                }
            }
            if (array == null) {
                return Collections.emptyList();
            }
            Type type = new TypeToken<List<OnlineServer>>() {
            }.getType();
            return GSON.fromJson(array, type);
        } catch (Exception e) {
            log.warn("拉取服务器列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * IP 匹配：先按字符串直接比较，再尝试将记录值按域名解析比对（记录 ip 可能是域名）
     */
    private static boolean ipEquals(String recordIp, String targetIp) {
        if (recordIp == null || targetIp == null) {
            return false;
        }
        if (StrUtil.equalsIgnoreCase(recordIp.trim(), targetIp.trim())) {
            return true;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(recordIp.trim())) {
                if (StrUtil.equalsIgnoreCase(address.getHostAddress(), targetIp.trim())) {
                    return true;
                }
            }
        } catch (Exception ignore) {
            // 域名解析失败按不匹配处理
        }
        return false;
    }
}
