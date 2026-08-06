package cn.xeblog.plugin.persistence;

import cn.hutool.core.bean.BeanUtil;
import cn.xeblog.commons.constants.Commons;
import cn.xeblog.plugin.backgroundImage.BackgroundService;
import cn.xeblog.plugin.backgroundImage.RandomBackgroundTask;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.tools.read.ReadConfig;
import cn.xeblog.plugin.webview.WebViewConst;
import cn.xeblog.plugin.util.CommandHistoryUtils;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WebView 版持久化服务。
 *
 * @author anlingyi
 */
@Slf4j
@State(name = Commons.KEY_PREFIX + "data", storages = {@Storage(Commons.KEY_PREFIX + "data.xml")})
public class PersistenceService implements PersistentStateComponent<PersistenceData> {

    private static PersistenceData data = new PersistenceData();

    @Override
    public @Nullable PersistenceData getState() {
        data.setUsername(DataCache.username);
        data.setMsgNotify(DataCache.msgNotify);
        data.setReadConfig(DataCache.readConfig);
        data.setHistoryCommandList(CommandHistoryUtils.getHistoryList());
        data.setBrowserConfig(DataCache.browserConfig);
        data.setUuid(DataCache.uuid);
        return data;
    }

    @Override
    public void loadState(@NotNull PersistenceData state) {
        data = state;
        DataCache.uuid = data.getUuid();
        DataCache.username = data.getUsername();
        DataCache.msgNotify = data.getMsgNotify();
        DataCache.readConfig = ReadConfig.getInstance(state.getReadConfig());
        BeanUtil.copyProperties(data.getBrowserConfig(), DataCache.browserConfig);
        CommandHistoryUtils.setHistoryList(state.getHistoryCommandList());
        ApplicationManager.getApplication().invokeLater(this::initRandomImage);
    }

    public void initRandomImage() {
        RandomBackgroundTask task = new RandomBackgroundTask();
        task.run();
        PropertiesComponent prop = PropertiesComponent.getInstance();
        if (prop.getBoolean(WebViewConst.AUTO_CHANGE, false)) {
            BackgroundService.restart();
        }
    }

    public static PersistenceData getData() {
        return data;
    }
}
