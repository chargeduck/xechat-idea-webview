package cn.xeblog.plugin.factory;

import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.webview.WebViewPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.net.URL;

/**
 * WebView 版 ToolWindow 工厂。
 * 主面板使用 IDEA 内置 JCEF（无需下载），视频工具 BROWSER2 按需加载 JxBrowser。
 *
 * @author anlingyi
 */
public class MainWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DataCache.project = project;

        JPanel container = new JPanel(new java.awt.BorderLayout());
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        Content content = contentFactory.createContent(container, "", false);
        toolWindow.getContentManager().addContent(content);

        // 将 classpath 资源 URL 转为 CEF 兼容的 file:/// 格式
        URL resource = getClass().getResource("/web/index.html");
        String webUrl = "about:blank";
        if (resource != null) {
            try {
                webUrl = new File(resource.toURI()).toURI().toString();
            } catch (Exception e) {
                webUrl = resource.toString();
            }
        }

        WebViewPanel webView = WebViewPanel.getInstance();
        webView.init(webUrl);

        SwingUtilities.invokeLater(() -> {
            container.add(webView, java.awt.BorderLayout.CENTER);
            container.revalidate();
            container.repaint();
        });
    }
}
