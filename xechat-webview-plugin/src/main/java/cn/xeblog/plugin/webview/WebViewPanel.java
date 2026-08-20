package cn.xeblog.plugin.webview;

import cn.xeblog.plugin.webview.bridge.JSBridge;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBuilder;
import com.intellij.ui.jcef.JBCefClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.*;
import java.awt.*;

/**
 * WebView 主面板，基于 IDEA 内置 JCEF 承载 Vue 前端。
 * 单例模式，全局共享一个 JBCefBrowser。
 *
 * @author anlingyi
 */
@Slf4j
public class WebViewPanel extends JPanel {

    private static volatile WebViewPanel instance;

    @Getter
    private JBCefBrowser browser;
    private JSBridge jsBridge;
    private String currentUrl;
    private volatile boolean initialized = false;

    private WebViewPanel() {
        setLayout(new BorderLayout());
    }

    public static WebViewPanel getInstance() {
        if (instance == null) {
            synchronized (WebViewPanel.class) {
                if (instance == null) {
                    instance = new WebViewPanel();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化 JCEF 并加载指定 URL。
     * JCEF 是 IDEA 内置浏览器，无需额外下载或 License。
     */
    public void init(String url) {
        if (initialized) {
            if (browser != null && !url.equals(currentUrl)) {
                browser.getCefBrowser().loadURL(url);
                currentUrl = url;
            }
            return;
        }

        this.currentUrl = url;

        SwingUtilities.invokeLater(() -> {
            try {
                browser = new JBCefBrowserBuilder().build();
                // IDEA 2025.2+ 要求：创建 JBCefJSQuery 前必须设置 pool size
                browser.getJBCefClient().setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 10);

                JComponent component = browser.getComponent();
                add(component, BorderLayout.CENTER);
                revalidate();
                repaint();

                jsBridge = new JSBridge(this);

                JBCefClient client = browser.getJBCefClient();
                CefBrowser cefBrowser = browser.getCefBrowser();
                if (cefBrowser != null) {
                    // CefMessageRouter 必须在 loadURL 前注册，否则渲染进程不会创建 window.cefQuery
                    log.info("JCEF WebView 开始注册 JSBridge");
                    jsBridge.setupMessageRouter();

                    // 自动打开 JCEF DevTools（调试用），失败不影响主流程
                    try {
                        browser.openDevtools();
                        log.info("JCEF DevTools 已自动打开");
                    } catch (Exception e) {
                        log.warn("JCEF DevTools 打开失败: {}", e.getMessage());
                    }

                    client.addLoadHandler(new CefLoadHandlerAdapter() {
                        @Override
                        public void onLoadEnd(CefBrowser cb, CefFrame frame, int httpStatusCode) {
                            if (frame.isMain()) {
                                SwingUtilities.invokeLater(() -> onPageLoaded());
                            }
                        }
                    }, cefBrowser);
                    cefBrowser.loadURL(url);

                    // 兜底：onLoadEnd 若因时序/缓存未触发，页面加载完成后强制注册 JSBridge，
                    // 避免 window.xechat 注入永远不生效（前端已另有 cefQuery 直连兜底）。
                    scheduleRegisterRetry();
                } else {
                    log.error("JCEF CefBrowser 为 null，无法加载页面");
                }
                initialized = true;

                log.info("JCEF WebView 初始化成功");

            } catch (Exception e) {
                log.error("JCEF 初始化失败", e);
                removeAll();
                JLabel errorLabel = new JLabel(
                        "<html><div style='text-align:center;padding:30px;'>" +
                                "<h2>JCEF 初始化失败</h2>" +
                                "<p>" + e.getMessage() + "</p>" +
                                "<p>请确认 IDE 支持 JCEF（IntelliJ IDEA 2020.2+）</p>" +
                                "</div></html>",
                        SwingConstants.CENTER);
                add(errorLabel, BorderLayout.CENTER);
                revalidate();
                repaint();
            }
        });
    }

    /**
     * JCEF 页面加载完成后由外部调用，注册 JS Bridge 并注入 API。
     */
    public void onPageLoaded() {
        if (jsBridge != null && !jsBridge.isRegistered()) {
            jsBridge.register();
        }
    }

    /**
     * 兜底注册：onLoadEnd 可能因时序/缓存未触发（页面已加载但 register 未执行）。
     * 每 1s 检查一次，页面停止加载且未注册时强制注册；注册成功后自动停止。
     */
    private void scheduleRegisterRetry() {
        log.info("JCEF WebView 开始兜底注册 JSBridge");
        Timer timer = new Timer(1000, e -> {
            try {
                if (jsBridge == null || jsBridge.isRegistered()) {
                    ((Timer) e.getSource()).stop();
                    return;
                }
                if (browser == null || browser.getCefBrowser() == null
                        || browser.getCefBrowser().isLoading()) {
                    return; // 页面仍在加载，继续等待
                }
                log.warn("[WebView] onLoadEnd 未触发 register，执行兜底注册 JSBridge");
                onPageLoaded();
                if (jsBridge.isRegistered()) {
                    ((Timer) e.getSource()).stop();
                }
            } catch (Exception ex) {
                log.warn("[WebView] 兜底注册 JSBridge 异常: {}", ex.getMessage());
            }
        });
        timer.start();
    }

    /**
     * 导航到指定 URL
     */
    public void navigate(String url) {
        if (browser != null) {
            this.currentUrl = url;
            browser.getCefBrowser().loadURL(url);
        }
    }

    /**
     * 执行 JavaScript（调度到 EDT，满足 CEF 的线程要求）
     */
    public void executeJS(String js) {
        if (browser != null && browser.getCefBrowser() != null && initialized) {
            SwingUtilities.invokeLater(() -> {
                if (browser != null && browser.getCefBrowser() != null) {
                    browser.getCefBrowser().executeJavaScript(js,
                            browser.getCefBrowser().getURL(), 0);
                }
            });
        }
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized && browser != null;
    }

    /**
     * JCEF 是否可用（IDEA 是否已启动 CEF）
     */
    public static boolean isCefAvailable() {
        try {
            return CefApp.getState() != CefApp.CefAppState.NONE;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 JSBridge
     */
    public JSBridge getJSBridge() {
        return jsBridge;
    }

    /**
     * 关闭并释放资源
     */
    public void dispose() {
        if (jsBridge != null) {
            jsBridge.dispose();
        }
        if (browser != null) {
            browser.getJBCefClient().dispose();
        }
        initialized = false;
        instance = null;
    }
}
