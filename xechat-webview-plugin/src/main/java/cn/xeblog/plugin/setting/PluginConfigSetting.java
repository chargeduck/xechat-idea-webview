package cn.xeblog.plugin.setting;

import cn.xeblog.commons.constants.Commons;
import cn.xeblog.commons.util.ServerUtils;
import cn.xeblog.plugin.persistence.PersistenceService;
import cn.xeblog.plugin.util.NotifyUtils;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author anlingyi
 * @date 2022/7/23 5:58 PM
 */
public class PluginConfigSetting implements SearchableConfigurable {

    private JTextField tokenInput;
    private JTextField licenseInput;
    private JTextField fetchServerListUrlInput;

    @Override
    public @NotNull
    @NonNls String getId() {
        return Commons.KEY_PREFIX + "setting";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return null;
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel configPanel = new JPanel();
        configPanel.setLayout(null);

        JLabel tokenLabel = new JLabel("Token:");
        tokenLabel.setBounds(10, 0, 180, 30);
        configPanel.add(tokenLabel);

        tokenInput = new JPasswordField();
        tokenInput.setBounds(60, 0, 300, 30);
        tokenInput.setText(PersistenceService.getData().getToken());
        configPanel.add(tokenInput);

        JLabel licensePanel = new JLabel("jxLicense:");
        licensePanel.setBounds(10, 30, 180, 30);
        configPanel.add(licensePanel);

        licenseInput = new JTextField();
        licenseInput.setBounds(60, 30, 300, 30);
        licenseInput.setText(PersistenceService.getData().getJxBrowserLicense());
        configPanel.add(licenseInput);

        JButton licenseBtn = new JButton("申请jxBrowser许可");
        licenseBtn.addActionListener(e -> openApplyUrl());
        configPanel.add(licenseBtn);

        JLabel serverListUrlLabel = new JLabel("ServerListUrl:");
        serverListUrlLabel.setBounds(10, 60, 180, 30);
        configPanel.add(serverListUrlLabel);

        fetchServerListUrlInput = new JTextField();
        fetchServerListUrlInput.setBounds(60, 60, 500, 30);
        String savedUrl = PersistenceService.getData().getFetchServerListUrl();
        fetchServerListUrlInput.setText(savedUrl == null || savedUrl.isEmpty()
                ? ServerUtils.getFetchServerListUrl() : savedUrl);
        configPanel.add(fetchServerListUrlInput);
        return configPanel;
    }

    private void openApplyUrl() {
        String applyUrl = "https://teamdev.cn/jxbrowser/?utm_campaign=java-media-player&utm_medium=article&utm_source=baijiiahao#evaluate";
        try {
            URI uri = new URI(applyUrl);
            if (!Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                desktop.browse(uri);

            } else {
                String[] command = getCommandForBrowser(uri);
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.start();
            }
        } catch (URISyntaxException | IOException e) {
            NotifyUtils.error("打开浏览器失败，请自行访问", applyUrl);
        }
    }

    private static String[] getCommandForBrowser(URI uri) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"rundll32", "url.dll,FileProtocolHandler", uri.toString()};
        } else if (os.contains("mac")) {
            return new String[]{"open", uri.toString()};
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return new String[]{"xdg-open", uri.toString()};
        }
        throw new IllegalArgumentException("Unsupported operating system.");
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        if (tokenInput != null) {
            PersistenceService.getData().setToken(tokenInput.getText());
        }
        if (licenseInput!= null) {
            PersistenceService.getData().setJxBrowserLicense(licenseInput.getText());
        }
        if (fetchServerListUrlInput != null) {
            String url = fetchServerListUrlInput.getText();
            PersistenceService.getData().setFetchServerListUrl(url);
            ServerUtils.setFetchServerListUrl(url);
        }
        System.out.println(PersistenceService.getData().getToken());
    }

}
