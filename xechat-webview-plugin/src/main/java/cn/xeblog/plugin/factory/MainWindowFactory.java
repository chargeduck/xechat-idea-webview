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
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * WebView 版 ToolWindow 工厂。
 * JCEF 通过 file:/// 协议加载本地文件。
 * 前端构建使用 IIFE 格式（非 ES module），以兼容 file:/// 协议的 CORS 限制。
 * runIde sandbox 模式下资源在 JAR 内，自动提取到临时目录后加载。
 *
 * @author anlingyi
 */
@Slf4j
public class MainWindowFactory implements ToolWindowFactory {

    /** 前端资源提取目标临时目录，设为静态字段确保同次调试会话复用 */
    private static Path extractedWebDir;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DataCache.project = project;

        JPanel container = new JPanel(new java.awt.BorderLayout());
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        Content content = contentFactory.createContent(container, "", false);
        toolWindow.getContentManager().addContent(content);

        String webUrl = resolveWebUrl();

        WebViewPanel webView = WebViewPanel.getInstance();
        webView.init(webUrl);

        SwingUtilities.invokeLater(() -> {
            container.add(webView, java.awt.BorderLayout.CENTER);
            container.revalidate();
            container.repaint();
        });
    }

    /**
     * 解析前端页面 URL。
     * - 开发模式（IDE 编译输出）：资源在文件系统，直接用 file:/// 加载
     * - runIde sandbox 模式（JAR 内）：提取 /web/ 到临时目录，file:/// 加载
     */
    private static String resolveWebUrl() {
        URL resource = MainWindowFactory.class.getResource("/web/index.html");
        if (resource == null) {
            log.error("资源 /web/index.html 未找到，请确认前端已构建");
            return "about:blank";
        }

        try {
            if ("file".equals(resource.getProtocol())) {
                // 开发模式：资源直接在文件系统中
                String url = new java.io.File(resource.toURI()).toURI().toString();
                log.info("加载前端页面（文件系统）: {}", url);
                return url;
            }

            // JAR 内资源：提取到临时目录（同次会话复用）
            if (extractedWebDir != null) {
                String url = extractedWebDir.resolve("web/index.html").toUri().toString();
                log.info("加载前端页面（缓存临时目录）: {}", url);
                return url;
            }

            Path tempDir = Files.createTempDirectory("xechat-webview-");
            tempDir.toFile().deleteOnExit();

            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("web/")) {
                        Path target = tempDir.resolve(entry.getName());
                        if (entry.isDirectory()) {
                            Files.createDirectories(target);
                        } else {
                            Files.createDirectories(target.getParent());
                            try (InputStream is = jarFile.getInputStream(entry)) {
                                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            }

            extractedWebDir = tempDir;
            String url = tempDir.resolve("web/index.html").toUri().toString();
            log.info("加载前端页面（从 JAR 提取到 {}）", tempDir);
            return url;
        } catch (Exception e) {
            log.error("解析前端资源 URL 失败", e);
            return "about:blank";
        }
    }
}
