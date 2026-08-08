package cn.xeblog.plugin.util;

import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.webview.WebViewPanel;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

/**
 * WebView 架构下的通知工具。
 * 控制台输出改为 VitePress ::: 容器格式，由 MarkdownMessage 渲染。
 * 同时推送前端 xechat:notify 事件 + 弹出 IntelliJ 原生通知。
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

        // 控制台输出 VitePress ::: 容器格式，由 MarkdownMessage 渲染
        String vtContent = content != null && !content.isEmpty() ? "\n" + content : "";
        ConsoleAction.showSimpleMsg("::: " + level + " " + title + vtContent + "\n:::");

        // IntelliJ 原生通知弹窗
        showIntelliJNotification(title, content, level);
    }

    private static void showIntelliJNotification(String title, String content, String level) {
        try {
            NotificationType type = switch (level) {
                case "warn" -> NotificationType.WARNING;
                case "error" -> NotificationType.ERROR;
                default -> NotificationType.INFORMATION;
            };
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            Project project = projects.length > 0 ? projects[0] : null;
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("cn.xeblog.xechat.webview.notify")
                    .createNotification(title, content, type)
                    .notify(project);
        } catch (Exception ignored) {
            // Notification API 在不同平台版本可能有兼容差异，静默降级
        }
    }
}
