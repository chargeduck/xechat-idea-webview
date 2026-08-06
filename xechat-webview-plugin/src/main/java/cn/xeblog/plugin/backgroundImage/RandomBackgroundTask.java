package cn.xeblog.plugin.backgroundImage;

import cn.hutool.core.util.RandomUtil;
import cn.xeblog.plugin.webview.WebViewConst;
import cn.xeblog.plugin.util.NotifyUtils;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.wm.impl.IdeBackgroundUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * WebView 版随机背景图任务。
 *
 * @author Allan de Queiroz
 */
@Slf4j
public class RandomBackgroundTask implements Runnable {

    private ImagesHandler imagesHandler;

    public RandomBackgroundTask() {
        imagesHandler = new ImagesHandler();
    }

    @Override
    public void run() {
        log.info("当前开始执行");
        PropertiesComponent prop = PropertiesComponent.getInstance();
        String folder = prop.getValue(WebViewConst.FOLDER);
        if (folder == null || folder.isEmpty()) {
            NotifyUtils.info(WebViewConst.GAME_NAME, "Image folder not set");
            return;
        }
        File file = new File(folder);
        if (!file.exists()) {
            NotifyUtils.info(WebViewConst.GAME_NAME, "Image folder not set");
            return;
        }
        String image = imagesHandler.getRandomImage(folder);
        NotifyUtils.info(WebViewConst.GAME_NAME, "当前地址" + image);
        if (image == null) {
            NotifyUtils.info(WebViewConst.GAME_NAME, "No image found");
            return;
        }
        if (image.contains(",")) {
            NotifyUtils.info(WebViewConst.GAME_NAME, "Intellij wont load images with ',' character\n" + image);
        }
        prop.setValue(IdeBackgroundUtil.FRAME_PROP, null);
        prop.setValue(IdeBackgroundUtil.EDITOR_PROP, image);
        try {
            IdeBackgroundUtil.repaintAllWindows();
        } catch (Exception e) {
            try {
                Thread.sleep(RandomUtil.randomInt(100));
            } catch (InterruptedException ex) {
                NotifyUtils.error(WebViewConst.GAME_NAME, ex.getMessage());
            }
            IdeBackgroundUtil.repaintAllWindows();
        }
    }
}
