package cn.xeblog.commons.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.xeblog.commons.entity.OnlineServer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * 服务器工具类
 *
 * @author nn200433
 * @date 2022-07-12 012 08:10:36
 */
public class ServerUtils {

    /**
     * 服务器列表拉取地址（可配置）。
     * 默认从 dld.lesscoding.net 拉取（nginx 反代到 xechat-manager /api/server/list）；
     * 可通过 JVM 参数 -Dxechat.serverListUrl=xxx 或 setFetchServerListUrl() 覆盖。
     */
    private static volatile String fetchServerListUrl = System.getProperty("xechat.serverListUrl",
            "https://dld.lesscoding.net/api/server/list");

    public static String getFetchServerListUrl() {
        return fetchServerListUrl;
    }

    public static void setFetchServerListUrl(String url) {
        if (StrUtil.isNotBlank(url)) {
            fetchServerListUrl = url.trim();
        }
    }

    /**
     * 获取服务器列表
     *
     * @return {@link List }<{@link OnlineServer }>
     * @author nn200433
     */
    public static List<OnlineServer> getServerList() {
        String resp = HttpUtil.get(fetchServerListUrl);
        if (StrUtil.isBlank(resp)) {
            return List.of();
        }
        try {
            JsonElement element = JsonParser.parseString(resp);
            if (element.isJsonArray()) {
                return toServerList(element.getAsJsonArray());
            }
            // 兼容后端统一 Result 包装：{code, msg, data: [...]}
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                JsonElement data = obj.get("data");
                if (data != null && data.isJsonArray()) {
                    return toServerList(data.getAsJsonArray());
                }
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private static List<OnlineServer> toServerList(JsonArray arr) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<OnlineServer>>() {}.getType();
        return gson.fromJson(arr, type);
    }

}
