package cn.xeblog.plugin.webview;

import cn.xeblog.plugin.persistence.PersistenceService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * 视频播放面板，基于 JxBrowser（支持 H.264）。
 * 仅当用户打开 BROWSER2 工具时才初始化，使用 JxBrowserLoader 延迟加载 JAR。
 * 使用独立 JFrame 而非 ToolWindow 嵌入，与主 JCEF WebView 隔离。
 *
 * @author anlingyi
 */
@Slf4j
public class VideoPlayerPanel {

    private static volatile VideoPlayerPanel instance;
    private volatile boolean open = false;

    private VideoPlayerPanel() {}

    public static VideoPlayerPanel getInstance() {
        if (instance == null) {
            synchronized (VideoPlayerPanel.class) {
                if (instance == null) {
                    instance = new VideoPlayerPanel();
                }
            }
        }
        return instance;
    }

    /**
     * 打开视频播放器窗口。延迟加载 JxBrowser。
     */
    public void open() {
        if (open) return;
        open = true;

        String licenseKey = PersistenceService.getData().getJxBrowserLicense();
        if (licenseKey == null || licenseKey.isBlank()) {
            // 通过主 WebView 提示用户配置 License
            WebViewPanel mainPanel = WebViewPanel.getInstance();
            if (mainPanel.isInitialized() && mainPanel.getJSBridge() != null) {
                mainPanel.getJSBridge().pushMessage(
                        "JxBrowser License 未配置，无法使用视频功能。" +
                        "请在 File > Settings > Tools > XEChat WebView > jxLicense 中填写。");
            }
            open = false;
            return;
        }

        JxBrowserLoader.ensureLoaded(
                () -> ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        JxWebViewWindow window = new JxWebViewWindow(licenseKey);
                        window.setVisible(true);
                        log.info("视频播放器窗口已打开");
                    } catch (Exception e) {
                        log.error("打开视频播放器失败", e);
                        WebViewPanel mainPanel = WebViewPanel.getInstance();
                        if (mainPanel.isInitialized() && mainPanel.getJSBridge() != null) {
                            mainPanel.getJSBridge().pushMessage("视频播放器初始化失败: " + e.getMessage());
                        }
                        open = false;
                    }
                }),
                errorMsg -> ApplicationManager.getApplication().invokeLater(() -> {
                    WebViewPanel mainPanel = WebViewPanel.getInstance();
                    if (mainPanel.isInitialized() && mainPanel.getJSBridge() != null) {
                        mainPanel.getJSBridge().pushMessage("JxBrowser 加载失败: " + errorMsg);
                    }
                    open = false;
                })
        );
    }

    public void onClosed() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }
}
