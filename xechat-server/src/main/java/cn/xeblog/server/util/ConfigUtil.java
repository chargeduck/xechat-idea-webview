package cn.xeblog.server.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.xeblog.commons.util.ParamsUtils;
import cn.xeblog.server.config.ServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配置工具类
 *
 * @author nn200433
 * @date 2022-07-21 021 08:55:52
 */
@Slf4j
public class ConfigUtil {

    /**
     * 读取配置
     *
     * @param args 参数
     * @return {@link ServerConfig }
     * @author nn200433
     */
    public static ServerConfig readConfig(String[] args) {
        final String configPort = ParamsUtils.getValue(args, "-p");
        final String sensitiveWordFilePath = ParamsUtils.getValue(args, "-swfile");
        final String weatherKey = ParamsUtils.getValue(args, "-weather");
        final String translationAppId = ParamsUtils.getValue(args, "-fyAppId");
        final String translationAppKey = ParamsUtils.getValue(args, "-fyAppKey");
        final String ip2regionPath = ParamsUtils.getValue(args, "-ipfile");
        final String configPath = ParamsUtils.getValue(args, "-path");
        final String token = ParamsUtils.getValue(args, "-token");
        final String enableWS = ParamsUtils.getValue(args, "-enableWS");
        final String forwardHost = ParamsUtils.getValue(args, "-fh");
        final String forwardPort = ParamsUtils.getValue(args, "-fp");
        final String serverName = ParamsUtils.getValue(args, "-sn");

        Map<String, Object> cfg = loadYmlConfig(configPath);
        final String fileConfigPort = resolvePlaceholders(getString(cfg, "server.port"));
        final String fileSensitiveWordFilePath = resolvePlaceholders(getString(cfg, "sensitive-word.file"));
        final String fileWeatherKey = resolvePlaceholders(getString(cfg, "weather.key"));
        final String fileTranslationAppId = resolvePlaceholders(getString(cfg, "translation.appId"));
        final String fileTranslationAppKey = resolvePlaceholders(getString(cfg, "translation.appKey"));
        final String fileIp2regionPath = resolvePlaceholders(getString(cfg, "ip-search.ip2Region_path"));
        final String fileToken = resolvePlaceholders(getString(cfg, "admin.token"));
        final String fileEnableWS = resolvePlaceholders(getString(cfg, "server.enableWS"));
        final String fileForwardHost = resolvePlaceholders(getString(cfg, "forward.host"));
        final String fileForwardPort = resolvePlaceholders(getString(cfg, "forward.port"));
        final String fileServerName = resolvePlaceholders(getString(cfg, "forward.serverName"));
        final String filePublicIp = resolvePlaceholders(getString(cfg, "server.publicIp"));

        return ServerConfig.builder()
                .port(Convert.toInt(StrUtil.blankToDefault(configPort, fileConfigPort), 1024))
                .sensitiveWordPath(StrUtil.blankToDefault(sensitiveWordFilePath, fileSensitiveWordFilePath))
                .weatherApiKey(StrUtil.blankToDefault(weatherKey, fileWeatherKey))
                .translationAppId(StrUtil.blankToDefault(translationAppId, fileTranslationAppId))
                .translationAppKey(StrUtil.blankToDefault(translationAppKey, fileTranslationAppKey))
                .ip2RegionPath(StrUtil.blankToDefault(ip2regionPath, fileIp2regionPath))
                .token(StrUtil.blankToDefault(token, fileToken))
                .enableWS(BooleanUtil.toBoolean(StrUtil.blankToDefault(enableWS, fileEnableWS)))
                .forwardHost(StrUtil.blankToDefault(forwardHost, fileForwardHost))
                .forwardPort(Convert.toInt(StrUtil.blankToDefault(forwardPort, fileForwardPort), 9527))
                .serverName(StrUtil.blankToDefault(serverName, fileServerName))
                .publicIp(filePublicIp)
                .build();
    }


    /**
     * 内置默认配置文件名（jar 内 classpath 兜底）
     */
    private static final String DEFAULT_YML = "application.yml";
    private static final String DEFAULT_YAML = "application.yaml";

    /**
     * ${ENV} 占位符
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * 加载 yml 配置，外部优先：
     * 1. -path 显式指定的 yml 文件（若为已弃用的 .setting 文件则给出提示并跳过）
     * 2. 工作目录下的 application.yml / application.yaml
     * 3. jar 内置 classpath 下的 application.yml
     */
    private static Map<String, Object> loadYmlConfig(String configPath) {
        if (StrUtil.isNotBlank(configPath)) {
            Path specified = Path.of(configPath);
            String lower = configPath.toLowerCase();
            if (lower.endsWith(".setting")) {
                log.warn("检测到 -path 指向已弃用的 config.setting，请改用 application.yml 配置，本次将按默认优先级加载 yml");
            } else if (Files.isRegularFile(specified)) {
                Map<String, Object> cfg = loadYml(specified);
                log.info("加载外部配置: {}", specified.toAbsolutePath().normalize());
                return cfg;
            } else {
                // 兼容 -path 传 classpath 内资源名
                try (InputStream in = ConfigUtil.class.getClassLoader().getResourceAsStream(configPath)) {
                    if (in != null) {
                        log.info("加载 classpath 配置: {}", configPath);
                        return loadYml(in);
                    }
                } catch (IOException e) {
                    log.warn("读取配置失败: {}", configPath, e);
                }
                log.warn("-path 指定的配置文件不存在: {}，按默认优先级加载 yml", configPath);
            }
        }

        // 外部配置优先：先找工作目录下的 application.yml / application.yaml
        for (String name : new String[]{DEFAULT_YML, DEFAULT_YAML}) {
            Path external = Path.of(System.getProperty("user.dir"), name);
            if (Files.isRegularFile(external)) {
                log.info("加载外部配置: {}", external.toAbsolutePath().normalize());
                return loadYml(external);
            }
        }

        // jar 内置默认配置兜底
        try (InputStream in = ConfigUtil.class.getClassLoader().getResourceAsStream(DEFAULT_YML)) {
            if (in != null) {
                log.info("未找到外部配置，加载 jar 内置默认配置: classpath:{}", DEFAULT_YML);
                return loadYml(in);
            }
        } catch (IOException e) {
            log.warn("读取内置默认配置失败", e);
        }
        log.warn("未找到任何 yml 配置（外部与内置均缺失），将全部使用启动参数与默认值");
        return Map.of();
    }

    private static Map<String, Object> loadYml(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return loadYml(in);
        } catch (IOException e) {
            throw new IllegalStateException("读取 yml 配置失败: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYml(InputStream in) {
        Object loaded = new Yaml().load(in);
        if (loaded == null) {
            return Map.of();
        }
        if (!(loaded instanceof Map)) {
            throw new IllegalStateException("yml 配置根节点必须为 Map");
        }
        return (Map<String, Object>) loaded;
    }

    /**
     * 按点分路径从 yml 嵌套 Map 中取值，如 server.port
     */
    @SuppressWarnings("unchecked")
    private static String getString(Map<String, Object> cfg, String dottedPath) {
        Object current = cfg;
        for (String part : dottedPath.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
        }
        return current == null ? null : String.valueOf(current);
    }

    /**
     * 解析 ${ENV} 占位符：优先系统属性，其次环境变量；均不存在时保留原样（由 ServerConfig 判空处理）
     */
    private static String resolvePlaceholders(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String resolved = System.getProperty(key);
            if (resolved == null) {
                resolved = System.getenv(key);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(resolved == null ? matcher.group(0) : resolved));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
