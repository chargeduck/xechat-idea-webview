package cn.xeblog.plugin.webview;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.engine.ProprietaryFeature;
import com.teamdev.jxbrowser.view.swing.BrowserView;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED;

/**
 * JxBrowser 独立视频窗口。
 * 仅在 BROWSER2 工具触发时按需创建，关闭即释放 JxBrowser 资源。
 *
 * @author anlingyi
 */
@Slf4j
public class JxWebViewWindow extends JFrame {

    private Engine engine;
    private Browser browser;

    public JxWebViewWindow(String licenseKey) {
        super("XEChat 视频播放器");
        setSize(1024, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        try {
            EngineOptions options = EngineOptions.newBuilder(HARDWARE_ACCELERATED)
                    .enableProprietaryFeature(ProprietaryFeature.AAC)
                    .enableProprietaryFeature(ProprietaryFeature.H_264)
                    .enableProprietaryFeature(ProprietaryFeature.WIDEVINE)
                    .licenseKey(licenseKey)
                    .build();

            engine = Engine.newInstance(options);
            browser = engine.newBrowser();

            BrowserView browserView = BrowserView.newInstance(browser);
            getContentPane().add(browserView, BorderLayout.CENTER);

            // 加载视频工具页面
            String toolUrl = getClass().getResource("/web/index.html") != null
                    ? getClass().getResource("/web/index.html").toString()
                    : "about:blank";
            browser.navigation().loadUrl(toolUrl);

        } catch (Exception e) {
            log.error("JxBrowser 视频窗口初始化失败", e);
            getContentPane().add(new JLabel(
                    "<html><p style='color:red'>JxBrowser 初始化失败: " +
                    e.getMessage() + "</p></html>",
                    SwingConstants.CENTER));
        }

        // 关闭窗口时释放 JxBrowser 资源
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                disposeBrowser();
                VideoPlayerPanel.getInstance().onClosed();
            }
        });
    }

    private void disposeBrowser() {
        if (browser != null) {
            try { browser.close(); } catch (Exception ex) { log.warn("关闭 Browser 失败", ex); }
        }
        if (engine != null) {
            try { engine.close(); } catch (Exception ex) { log.warn("关闭 Engine 失败", ex); }
        }
    }
}
