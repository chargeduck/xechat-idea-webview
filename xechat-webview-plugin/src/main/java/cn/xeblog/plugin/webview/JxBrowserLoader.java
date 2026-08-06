package cn.xeblog.plugin.webview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * JxBrowser 动态加载器。
 * 插件分发包不含 JxBrowser JAR，首次使用时从内置资源或 CDN 下载到插件 lib 目录，
 * 通过自定义 URLClassLoader 加载，实现插件包体积最小化。
 *
 * @author anlingyi
 */
public class JxBrowserLoader {

    private static final Logger LOG = Logger.getInstance(JxBrowserLoader.class);

    private static final String JXBROWSER_VERSION = "7.22";

    /**
     * JxBrowser JAR 文件名列表（按加载顺序）
     */
    private static final String[] JAR_NAMES = {
            "jxbrowser-" + JXBROWSER_VERSION + ".jar",
            "jxbrowser-swing-" + JXBROWSER_VERSION + ".jar",
            "jxbrowser-" + getPlatformId() + "-" + JXBROWSER_VERSION + ".jar"
    };

    private static volatile boolean loaded = false;
    private static volatile String errorMessage;

    /**
     * 确保 JxBrowser 已加载。如果未加载，异步下载并加载。
     *
     * @param onSuccess 加载成功回调
     * @param onError   加载失败回调
     */
    public static void ensureLoaded(Runnable onSuccess, java.util.function.Consumer<String> onError) {
        if (loaded) {
            ApplicationManager.getApplication().invokeLater(onSuccess);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Path libDir = getJxBrowserLibDir();
                Files.createDirectories(libDir);

                // 检查所有 JAR 是否存在
                List<URL> jarUrls = new ArrayList<>();
                for (String jarName : JAR_NAMES) {
                    Path jarPath = libDir.resolve(jarName);
                    if (!Files.exists(jarPath)) {
                        // 从插件内置资源提取
                        extractJarFromResources(jarName, jarPath);
                    }
                    jarUrls.add(jarPath.toUri().toURL());
                }

                // 创建 ClassLoader 并触发加载
                URLClassLoader loader = new URLClassLoader(
                        jarUrls.toArray(new URL[0]),
                        JxBrowserLoader.class.getClassLoader()
                );

                // 验证核心类可加载
                loader.loadClass("com.teamdev.jxbrowser.engine.Engine");
                loader.loadClass("com.teamdev.jxbrowser.view.swing.BrowserView");

                loaded = true;
                LOG.info("JxBrowser 动态加载成功");
                ApplicationManager.getApplication().invokeLater(onSuccess);

            } catch (Exception e) {
                errorMessage = "JxBrowser 加载失败: " + e.getMessage();
                LOG.error(errorMessage, e);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (onError != null) {
                        onError.accept(errorMessage);
                    }
                });
            }
        });
    }

    /**
     * 从插件资源中提取 JAR 到目标目录
     */
    private static void extractJarFromResources(String jarName, Path targetPath) throws IOException {
        // 尝试从 resources/jxbrowser/ 目录读取
        String resourcePath = "/jxbrowser/" + jarName;
        try (InputStream in = JxBrowserLoader.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("从插件资源提取 JAR: " + jarName);
            } else {
                throw new IOException("JAR 资源不存在: " + resourcePath +
                        "，请确保已将 JxBrowser JAR 放入插件 resources/jxbrowser/ 目录");
            }
        }
    }

    /**
     * 获取 JxBrowser 库目录
     */
    private static Path getJxBrowserLibDir() {
        String pluginPath = com.intellij.openapi.application.PathManager.getPluginsPath();
        return Paths.get(pluginPath, "xechat-webview-plugin", "lib", "jxbrowser");
    }

    /**
     * 获取当前平台标识
     */
    private static String getPlatformId() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("win")) return "win64";
        if (os.contains("mac")) return "mac";
        if (os.contains("linux")) {
            return arch.contains("64") ? "linux64" : "linux32";
        }
        return "win64"; // 默认
    }

    /**
     * 是否已加载成功
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * 获取错误信息
     */
    public static String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 获取 JxBrowser JAR 版本
     */
    public static String getVersion() {
        return JXBROWSER_VERSION;
    }
}
