package cn.xeblog.server.config;

import cn.hutool.core.util.StrUtil;
import lombok.*;

/**
 * @author nn200433
 * @date 2022-07-21 021 08:31:14
 */
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfig {

    /**
     * 端口
     */
    @Getter
    private Integer port;

    /**
     * 是否开启WS协议
     */
    private boolean enableWS;

    /**
     * 敏感词文件地址
     */
    private String sensitiveWordPath;

    /**
     * 和风天气api key
     */
    private String weatherApiKey;

    /**
     * 百度翻译app id
     */
    private String translationAppId;

    /**
     * 百度翻译app key
     */
    private String translationAppKey;

    /**
     * 离线ip库地址
     */
    private String ip2RegionPath;

    /**
     * 管理员token
     */
    private String token;

    /**
     * 转发服务器（hub）host
     */
    private String forwardHost;

    /**
     * 转发服务器（hub）port
     */
    @Getter
    private Integer forwardPort;

    /**
     * 本服务器接入名（展示用，可为空由 hub 探测或按接入顺序分配）
     */
    private String serverName;

    /**
     * 注册重试上限（次），未配置默认 3（yml: forward.maxRetry）
     */
    private Integer forwardMaxRetry;

    /**
     * 单次建连超时（毫秒），未配置默认 3000（yml: forward.timeout）
     */
    private Integer forwardTimeout;

    @Setter
    private static ServerConfig serverConfig;

    public static ServerConfig getConfig() {
        if (serverConfig == null) {
            serverConfig = new ServerConfig();
        }

        return serverConfig;
    }

    public String getSensitiveWordPath() {
        return StrUtil.equals("${SW_FILE}", sensitiveWordPath) ? null : sensitiveWordPath;
    }

    public String getWeatherApiKey() {
        return StrUtil.equals("${WEATHER_KEY}", weatherApiKey) ? null : weatherApiKey;
    }

    public String getTranslationAppId() {
        return StrUtil.equals("${BD_APP_ID}", translationAppId) ? null : translationAppId;
    }

    public String getTranslationAppKey() {
        return StrUtil.equals("${BD_APP_KEY}", translationAppKey) ? null : translationAppKey;
    }

    public String getIp2RegionPath() {
        return StrUtil.equals("${IP2REGION_PATH}", ip2RegionPath) ? null : ip2RegionPath;
    }

    public String getToken() {
        return StrUtil.equals("${TOKEN}", token) ? null : token;
    }

    public boolean getEnableWS() {
        return enableWS;
    }

    public String getForwardHost() {
        return StrUtil.equals("${FORWARD_HOST}", forwardHost) ? null : forwardHost;
    }

    public String getServerName() {
        return StrUtil.equals("${SERVER_NAME}", serverName) ? null : serverName;
    }

    public Integer getForwardMaxRetry() {
        return forwardMaxRetry;
    }

    public Integer getForwardTimeout() {
        return forwardTimeout;
    }
}
