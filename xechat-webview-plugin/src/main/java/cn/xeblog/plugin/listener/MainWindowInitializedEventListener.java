package cn.xeblog.plugin.listener;

/**
 * WebView 版主窗口初始化完成事件监听器。
 * 移除 MainWindow 参数，WebView 模式下不再需要 Swing 组件引用。
 *
 * @author anlingyi
 */
public interface MainWindowInitializedEventListener {

    /**
     * 插件窗口初始化完成后的回调。
     */
    void afterInit();
}
