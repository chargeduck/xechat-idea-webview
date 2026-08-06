package cn.xeblog.plugin.util;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.xeblog.commons.entity.react.React;
import cn.xeblog.commons.entity.react.request.UploadReact;
import cn.xeblog.commons.entity.react.result.UploadReactResult;
import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.action.ReactAction;
import cn.xeblog.plugin.action.handler.ReactResultConsumer;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * 文件上传工具类（Vue 迁移版）。
 * 去掉 java.awt.Image / BufferedImage / ImageIO 引用，
 * 保留文件校验和上传逻辑。
 *
 * @author anlingyi
 * @date 2020/9/10
 */
public class UploadUtils {

    private static boolean UPLOADING;
    private static final String[] ACCEPT_IMAGE_TYPE = new String[]{
            ImgUtil.IMAGE_TYPE_GIF,
            ImgUtil.IMAGE_TYPE_JPG,
            ImgUtil.IMAGE_TYPE_JPEG,
            ImgUtil.IMAGE_TYPE_BMP,
            ImgUtil.IMAGE_TYPE_PNG
    };

    /**
     * 通过文件路径上传图片
     */
    public static void uploadImageFile(File file) {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            inputStream.mark(28);
            String fileType = FileTypeUtil.getType(inputStream);
            if (!ArrayUtil.contains(ACCEPT_IMAGE_TYPE, fileType)) {
                throw new Exception("不支持的图片类型！");
            }

            inputStream.reset();
            sendImgAsync(IOUtils.toByteArray(inputStream), generateFileName(fileType));
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleAction.showSimpleMsg(e.getMessage());
        }
    }

    /**
     * TODO: 通过图片字节数组上传（原 uploadImage(Image image) 方法移除 Image 参数后的替代）。
     * 前端 JSBridge 将图片转为 byte[] 后调用此方法。
     */
    public static void uploadImageBytes(byte[] imageBytes) {
        try {
            sendImgAsync(imageBytes, generateFileName("png"));
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleAction.showSimpleMsg("图片上传失败！");
        }
    }

    private static String generateFileName(String fileType) {
        fileType = fileType == null ? "jpg" : fileType;
        return IdUtil.fastUUID() + "." + fileType;
    }

    private static void sendImgAsync(byte[] bytes, String fileName) {
        if (UPLOADING) {
            ConsoleAction.showSimpleMsg("请等待之前的图片上传完成！");
            return;
        }

        UPLOADING = true;
        ConsoleAction.showSimpleMsg("图片上传中...");

        UploadReact uploadReact = new UploadReact();
        uploadReact.setFileType(fileName.substring(fileName.lastIndexOf(".") + 1));
        uploadReact.setBytes(bytes);
        ReactAction.request(uploadReact, React.UPLOAD, 600, new ReactResultConsumer<UploadReactResult>() {
            @Override
            public void doSucceed(UploadReactResult body) {
                UPLOADING = false;
                ConsoleAction.showSimpleMsg("图片上传成功！");
            }

            @Override
            public void doFailed(String msg) {
                UPLOADING = false;
                ConsoleAction.showSimpleMsg("图片上传失败！原因：" + msg);
            }
        });
    }
}
