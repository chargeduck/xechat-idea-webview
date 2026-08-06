package cn.xeblog.plugin.backgroundImage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Author: Allan de Queiroz
 * Date:   07/05/17
 * Modify: 移除废弃的MimetypesFileTypeMap，改用Java NIO标准API
 */
class ImagesHandler {

    // 直接移除废弃的typeMap成员变量，无需初始化

    ImagesHandler() {
        // 构造方法留空，无需要初始化操作
    }

    /**
     * @param folder folder to search for images
     * @return random image or null
     */
    String getRandomImage(String folder) {
        if (folder == null || folder.isEmpty()) { // 增加null判断，更健壮
            return null;
        }
        List<String> images = new ArrayList<>();
        collectImages(images, folder);
        if (images.isEmpty()) { // 简化判断，语义更清晰
            return null;
        }
        Random randomGenerator = new Random();
        int index = randomGenerator.nextInt(images.size());
        return images.get(index);
    }

    private void collectImages(List<String> images, String folder) {
        File root = new File(folder);
        if (!root.exists() || !root.isDirectory()) { // 增加是否为目录的判断，避免文件传进来报错
            return;
        }
        File[] list = root.listFiles();
        if (list == null) {
            return;
        }

        for (File f : list) {
            if (f.isDirectory()) {
                collectImages(images, f.getAbsolutePath());
            } else if (f.isFile() && isImage(f)) { // 增加是否为文件的判断，过滤特殊文件
                images.add(f.getAbsolutePath());
            }
        }
    }

    /**
     * 改用Java NIO的Files.probeContentType探测MIME类型，替代废弃的MimetypesFileTypeMap
     * 增加异常处理，避免文件无法读取时导致整个流程中断
     */
    private boolean isImage(File file) {
        // 前置判断：文件必须存在+是普通文件，否则直接返回false
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        try {
            // 探测文件MIME类型
            String mimeType = Files.probeContentType(Paths.get(file.getAbsolutePath()));
            // 判空后判断是否为图片类型（image/jpg、image/png等）
            return mimeType != null && mimeType.startsWith("image/");
        } catch (Exception e) {
            // 捕获IO异常/权限异常等，避免单个文件问题影响整体扫描
            return false;
        }
    }

}