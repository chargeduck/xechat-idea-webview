package cn.xeblog.plugin.tools.browser2.utils;

import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.engine.ProprietaryFeature;

import static com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED;

/**
 * @author eleven
 * @date 2024/11/18 9:27
 * @apiNote
 */
public class EngineUtil {
    public static EngineOptions engineOptions(String jxBrowserLicense) {
        return EngineOptions.newBuilder(HARDWARE_ACCELERATED)
                .enableProprietaryFeature(ProprietaryFeature.AAC)
                .addSwitch("--disable-extensions=false")
                .addSwitch("--disable-gpu=true")
                .enableProprietaryFeature(ProprietaryFeature.H_264)
                .enableProprietaryFeature(ProprietaryFeature.WIDEVINE)
                .licenseKey(jxBrowserLicense)
                .build();
    }

    public static Engine engine(String jxBrowserLicense) {
        return Engine.newInstance(engineOptions(jxBrowserLicense));
    }
}
