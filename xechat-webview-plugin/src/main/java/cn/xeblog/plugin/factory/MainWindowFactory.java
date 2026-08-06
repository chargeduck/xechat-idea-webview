package cn.xeblog.plugin.factory;

import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.webview.WebViewPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.URL;

/**
 * WebView 版 ToolWindow 工厂。
 * JCEF 直接加载 classpath 下 Vite 打包的 index.html。
 * 视频工具 BROWSER2 按需加载 JxBrowser。
 *
 * @author anlingyi
 */
@Slf4j
public class MainWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DataCache.project = project;

        JPanel container = new JPanel(new java.awt.BorderLayout());
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        Content content = contentFactory.createContent(container, "", false);
        toolWindow.getContentManager().addContent(content);

        URL resource = getClass().getResource("/web/index.html");
        String webUrl;
        if (resource == null) {
            log.error("资源 /web/index.html 未找到，请确认前端已构建");
            webUrl = "about:blank";
        } else {
            try {
                webUrl = resource.toURI().toString();
                log.info("加载前端页面: {}", webUrl);
            } catch (Exception e) {
                log.error("URL 转换失败", e);
                webUrl = "about:blank";
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
