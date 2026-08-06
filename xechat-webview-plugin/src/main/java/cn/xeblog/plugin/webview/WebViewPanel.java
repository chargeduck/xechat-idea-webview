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

                // JS Bridge 延迟注册：等页面加载完成后再注入
                jsBridge = new JSBridge(this);

                // 页面加载完成后自动注册 JS Bridge
                JBCefClient client = browser.getJBCefClient();
                CefBrowser cefBrowser = browser.getCefBrowser();
                if (cefBrowser != null) {
                    client.addLoadHandler(new CefLoadHandlerAdapter() {
                        @Override
                        public void onLoadEnd(CefBrowser cb, CefFrame frame, int httpStatusCode) {
                            if (frame.isMain()) {
                                SwingUtilities.invokeLater(() -> onPageLoaded());
                            }
                        }
                    }, cefBrowser);
                    cefBrowser.loadURL(url);
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
     * 导航到指定 URL
     */
    public void navigate(String url) {
        if (browser != null) {
            this.currentUrl = url;
            browser.getCefBrowser().loadURL(url);
        }
    }

    /**
     * 执行 JavaScript
     */
    public void executeJS(String js) {
        if (browser != null && browser.getCefBrowser() != null && initialized) {
            browser.getCefBrowser().executeJavaScript(js,
                    browser.getCefBrowser().getURL(), 0);
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
