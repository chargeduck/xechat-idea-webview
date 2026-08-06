package cn.xeblog.plugin.util;

import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.webview.WebViewPanel;

/**
 * WebView 架构下的通知工具。
 * 通知不再使用 IntelliJ 原生 Notification API（2.x 平台 classpath 存在兼容问题），
 * 改为推送到前端 WebView 显示弹窗通知，同时输出到控制台。
 *
 * @author anlingyi
 */
public class NotifyUtils {

    public final static String GROUP_ID = "cn.xeblog.xechat.notify";

    public static void info(String title, String content) {
        info(title, content, false);
    }

    public static void warn(String title, String content) {
        warn(title, content, false);
    }

    public static void error(String title, String content) {
        error(title, content, false);
    }

    public static void info(String title, String content, boolean checked) {
        notify(title, content, "info", checked);
    }

    public static void warn(String title, String content, boolean checked) {
        notify(title, content, "warn", checked);
    }

    public static void error(String title, String content, boolean checked) {
        notify(title, content, "error", checked);
    }

    public static void notify(String title, String content, String level, boolean checked) {
        if (checked) {
            switch (DataCache.msgNotify) {
                case 3:
                    return;
                case 2:
                    title = "";
                    content = "New Bug!";
                    break;
            }
        }

        // 推送到前端 WebView 显示通知弹窗
        WebViewPanel panel = WebViewPanel.getInstance();
        if (panel != null && panel.isInitialized()) {
            String json = new com.google.gson.Gson().toJson(
                    java.util.Map.of("title", title, "content", content, "level", level)
            );
            panel.executeJS(String.format(
                    "window.dispatchEvent(new CustomEvent('xechat:notify', { detail: %s }))",
                    json
            ));
        }

        // 同时输出到控制台
        ConsoleAction.showSimpleMsg("[" + level.toUpperCase() + "] " + title + ": " + content);
    }
}
