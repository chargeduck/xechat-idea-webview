package cn.xeblog.plugin.listener;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.*;
import org.jetbrains.annotations.NotNull;

/**
 * WebView 版项目事件监听器。
 * 移除 Swing MainWindow 依赖。
 *
 * @author anlingyi
 */
public class ProjectEventListener implements ProjectManagerListener, StartupActivity {

    private static final String WINDOW_ID = "XEChat";

    @Override
    public void projectClosed(@NotNull Project project) {
        IdeFrame[] allProjectFrames = WindowManager.getInstance().getAllProjectFrames();
        Project otherProject = allProjectFrames[0].getProject();
        if (otherProject != null) {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(otherProject);
            toolWindowManager.invokeLater(() -> {
                ToolWindow toolWindow = toolWindowManager.getToolWindow(WINDOW_ID);
                if (toolWindow != null) {
                    toolWindow.show();
                }
            });
        }
    }

    @Override
    public void runActivity(@NotNull Project project) {
        // WebView 模式：MainWindowFactory 在 createToolWindowContent 中初始化
        // 启动时无需额外操作
    }
}
