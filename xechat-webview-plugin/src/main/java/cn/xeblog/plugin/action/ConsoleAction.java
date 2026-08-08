package cn.xeblog.plugin.action;

import cn.xeblog.plugin.enums.Command;
import cn.xeblog.plugin.enums.Style;
import cn.xeblog.plugin.listener.MainWindowInitializedEventListener;
import cn.xeblog.plugin.mode.ModeContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

/**
 * WebView 版控制台操作类。
 * 维护文本缓冲区，通过 JSBridge 推送给前端 Vue 渲染。
 *
 * @author anlingyi
 */
@Slf4j
public class ConsoleAction implements MainWindowInitializedEventListener {

    private static final ReentrantLock LOCK = new ReentrantLock();

    /** 文本缓冲区，每条为一行消息 */
    private static final List<String> messageBuffer = new ArrayList<>();

    /** 缓冲区最大行数 */
    private static final int MAX_BUFFER_SIZE = 500;

    /** 获取当前缓冲区消息数（仅用于调试日志） */
    public static int getBufferSize() {
        LOCK.lock();
        try {
            return messageBuffer.size();
        } finally {
            LOCK.unlock();
        }
    }

    private static volatile boolean isNewLine = true;

    @Override
    public void afterInit() {
        // WebView 模式下无需获取 Swing 组件引用
    }

    /**
     * 获取并清空消息缓冲区（供 JSBridge 轮询或事件推送使用）
     */
    public static List<String> drainMessages() {
        LOCK.lock();
        try {
            List<String> result = new ArrayList<>(messageBuffer);
            if (!result.isEmpty()) {
                log.info("[ConsoleAction] drainMessages: {} 条消息", result.size());
            }
            messageBuffer.clear();
            return result;
        } finally {
            LOCK.unlock();
        }
    }

    public static void renderText(String text) {
        renderText(text, Style.DEFAULT);
    }

    public static void renderText(String text, Style style) {
        LOCK.lock();
        try {
            if (isNewLine) {
                ModeContext.getMode().renderTextBefore(text);
            }

            appendToBuffer(text, style);

            isNewLine = text.endsWith("\n");
        } finally {
            LOCK.unlock();
        }
    }

    public static void showSimpleMsg(String msg) {
        renderText(msg + "\n", Style.DEFAULT);
    }

    public static void showErrorMsg() {
        showSimpleMsg("输入的命令有误！帮助命令：#hlep");
    }

    public static void showLoginMsg() {
        showSimpleMsg("请先登录！登录命令：" + Command.LOGIN.getCommand() + "，帮助命令：#help" );
    }

    public static void showSystemMsg(String time, String msg) {
        renderText(String.format("[%s] 系统消息：%s\n", time, msg), Style.SYSTEM_MSG);
    }

    public static void clean() {
        LOCK.lock();
        try {
            messageBuffer.clear();
        } finally {
            LOCK.unlock();
        }
    }

    private static void appendToBuffer(String text, Style style) {
        String prefix = (style != null && style != Style.DEFAULT) ? style.name() + "::" : "";
        messageBuffer.add(prefix + text);

        while (messageBuffer.size() > MAX_BUFFER_SIZE) {
            messageBuffer.remove(0);
        }
    }

    // ==================== 兼容旧 API（WebView 模式空实现或前端桥接） ====================

    /** 带锁执行（兼容旧代码） */
    public static void atomicExec(Runnable runnable) {
        LOCK.lock();
        try {
            runnable.run();
        } finally {
            LOCK.unlock();
        }
    }

    /** 滚动到底部（WebView 模式前端自行处理） */
    public static void gotoConsoleLow() {}

    /** 刷新 UI（WebView 模式前端自行处理） */
    public static void updateUI() {}

    /** 设置控制台标题（WebView 模式前端自行处理） */
    public static void setConsoleTitle(String title) {}

    /** 渲染图片标签（WebView 模式改为文本占位） */
    public static void renderImageLabel(Object label) {
        renderText("[图片]\n");
    }

    /** 渲染链接（WebView 模式改为文本链接） */
    public static void renderUrl(String title, String url) {
        renderText(title + "(" + url + ")\n");
    }

    /** 渲染组件（WebView 模式忽略） */
    public static void renderComponent(Object component) {
        // WebView 模式不支持嵌入 Swing 组件
    }
}
