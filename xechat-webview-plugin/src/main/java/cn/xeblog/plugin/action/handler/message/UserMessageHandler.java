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
        String filePath = IMAGES_DIR + "/" + fileName;
        boolean existFile = new File(filePath).exists();

        ConsoleAction.atomicExec(() -> {
            renderName(response);
            if (existFile) {
                ConsoleAction.renderText("[查看图片](" + filePath + ")\n");
            } else {
                ConsoleAction.renderText("[图片下载中...]\n");
                String finalFilePath = filePath;
                GlobalThreadPool.execute(() -> {
                    ReactAction.request(new DownloadReact(fileName), React.DOWNLOAD, 300,
                            new ReactResultConsumer<DownloadReactResult>() {
                                @Override
                                public void doSucceed(DownloadReactResult result) {
                                    File imageFile = new File(finalFilePath);
                                    if (!imageFile.exists()) {
                                        FileUtil.mkdir(IMAGES_DIR);
                                        try (FileOutputStream out = new FileOutputStream(imageFile)) {
                                            out.write(result.getBytes());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    ConsoleAction.renderText("[图片已下载](" + finalFilePath + ")\n");
                                    ApplicationManager.getApplication().invokeLater(
                                            () -> OpenFileAction.openFile(finalFilePath, DataCache.project));
                                }

                                @Override
                                public void doFailed(String msg) {
                                    ConsoleAction.showSimpleMsg("图片下载失败！原因：" + msg);
                                }
                            });
                });
            }
        });
    }
}
