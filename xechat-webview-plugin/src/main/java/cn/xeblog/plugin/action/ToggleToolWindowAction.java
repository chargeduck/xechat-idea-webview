package cn.xeblog.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * 显示/隐藏 XEChat 工具窗口。
 * 不绑定默认快捷键，由用户在 IDEA Keymap 中自行设置
 * （Settings → Keymap → 搜索 “XEChat”）。
 *
 * @author anlingyi
 */
public class ToggleToolWindowAction extends AnAction {

    public static final String WINDOW_ID = "XEChat";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        if (toolWindow.isVisible()) {
            toolWindow.hide();
        } else {
            toolWindow.show();
        }
    }
}
