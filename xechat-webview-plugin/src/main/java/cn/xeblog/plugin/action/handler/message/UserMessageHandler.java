package cn.xeblog.plugin.action.handler.message;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.xeblog.commons.entity.Response;
import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.entity.UserMsgDTO;
import cn.xeblog.commons.entity.react.React;
import cn.xeblog.commons.entity.react.request.DownloadReact;
import cn.xeblog.commons.entity.react.result.DownloadReactResult;
import cn.xeblog.commons.enums.MessageType;
import cn.xeblog.commons.enums.Platform;
import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.action.ReactAction;
import cn.xeblog.plugin.action.handler.ReactResultConsumer;
import cn.xeblog.plugin.annotation.DoMessage;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.entity.Mask;
import cn.xeblog.plugin.enums.Style;
import cn.xeblog.plugin.util.NotifyUtils;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.application.ApplicationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * WebView 版用户消息处理器。
 *
 * @author anlingyi
 */
@DoMessage(MessageType.USER)
public class UserMessageHandler extends AbstractMessageHandler<UserMsgDTO> {

    private static final String IMAGES_DIR = System.getProperty("user.home") + "/xechat/images";

    @Override
    protected void process(Response<UserMsgDTO> response) {
        User user = response.getUser();
        UserMsgDTO body = response.getBody();
        Mask mask = DataCache.mask;
        List<String> maskIps = mask.getMaskIps();
        Boolean notShow = mask.getNotShow();
        if (maskIps.contains(user.getIp())) {
            if (notShow) {
                return;
            }
            body.setMsgType(UserMsgDTO.MsgType.TEXT);
            body.setContent("已被禁言，无法发言");
        }
        List<String> maskRegions = mask.getMaskRegions();
        if (maskRegions.contains(user.getShortRegion())) {
            if (notShow) {
                return;
            }
            body.setMsgType(UserMsgDTO.MsgType.TEXT);
            body.setContent("已被禁言，无法发言");
        }
        List<String> maskUsernames = mask.getMaskUsernames();
        if (maskUsernames.stream().anyMatch(item -> item.contains(user.getUsername()))) {
            if (notShow) {
                return;
            }
            body.setMsgType(UserMsgDTO.MsgType.TEXT);
            body.setContent("已被禁言，无法发言");
        }
        boolean isImage = body.getMsgType() == UserMsgDTO.MsgType.IMAGE;
        if (isImage) {
            renderImage(response);
        } else {
            ConsoleAction.atomicExec(() -> {
                renderName(response);
                boolean notified = body.hasUser(DataCache.username);
                Style style = Style.DEFAULT;
                String msg = (String) body.getContent();
                if (notified) {
                    style = Style.LIGHT;
                    if (!user.getUsername().equals(DataCache.username)) {
                        NotifyUtils.info(user.getUsername(), msg, true);
                    }
                }
                ConsoleAction.renderText(msg + "\n", style);
            });
        }
    }

    private void renderName(Response<UserMsgDTO> response) {
        User user = response.getUser();
        String platform = user.getPlatform() == Platform.WEB ? " \u0F04" : " \u2668";
        String roleDisplay = "";
        if (user.getRole() == User.Role.ADMIN) {
            roleDisplay = " \u2606";
        }

        ConsoleAction.renderText(
                String.format("[%s][%s] %s (%s)%s%s\uff1a",
                        response.getTime(),
                        user.getShortRegion(),
                        user.getUsername(),
                        user.getStatus().getName(),
                        platform,
                        roleDisplay), Style.USER_NAME);
    }

    private void renderImage(Response<UserMsgDTO> response) {
        UserMsgDTO body = response.getBody();
        String fileName = (String) body.getContent();

        ConsoleAction.atomicExec(() -> {
            renderName(response);
            // 仅渲染下载按钮，点击后由前端经 JSBridge 触发 downloadImage
            ConsoleAction.renderText("[下载图片](xechat-download://" + encodeFileName(fileName) + ")\n");
        });
    }

    /**
     * 下载图片：本地已存在则直接打开，否则从服务器下载后打开。
     * 由前端点击"下载图片"按钮经 JSBridge 调用。
     */
    public static void downloadImage(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        String filePath = IMAGES_DIR + "/" + fileName;
        if (new File(filePath).exists()) {
            ApplicationManager.getApplication().invokeLater(
                    () -> OpenFileAction.openFile(filePath, DataCache.project));
            return;
        }
        ConsoleAction.renderText("[图片下载中...]\n");
        GlobalThreadPool.execute(() -> {
            ReactAction.request(new DownloadReact(fileName), React.DOWNLOAD, 300,
                    new ReactResultConsumer<DownloadReactResult>() {
                        @Override
                        public void doSucceed(DownloadReactResult result) {
                            File imageFile = new File(filePath);
                            if (!imageFile.exists()) {
                                FileUtil.mkdir(IMAGES_DIR);
                                try (FileOutputStream out = new FileOutputStream(imageFile)) {
                                    out.write(result.getBytes());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            ConsoleAction.renderText("[图片已下载](" + filePath + ")\n");
                            ApplicationManager.getApplication().invokeLater(
                                    () -> OpenFileAction.openFile(filePath, DataCache.project));
                        }

                        @Override
                        public void doFailed(String msg) {
                            ConsoleAction.showSimpleMsg("图片下载失败！原因：" + msg);
                        }
                    });
        });
    }

    /** 文件名编码，避免破坏 xechat-download:// 协议解析 */
    private static String encodeFileName(String fileName) {
        return fileName
                .replace(" ", "%20")
                .replace("(", "%28")
                .replace(")", "%29")
                .replace("[", "%5B")
                .replace("]", "%5D");
    }
}
