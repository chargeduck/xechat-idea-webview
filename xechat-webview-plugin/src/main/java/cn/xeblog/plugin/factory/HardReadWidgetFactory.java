package cn.xeblog.plugin.factory;

import cn.xeblog.plugin.tools.read.ui.HardReadWidget;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class HardReadWidgetFactory implements StatusBarWidgetFactory {

    @NonNls
    public static final String ID = "HardReadWidget";

    @Override
    public @NonNls @NotNull String getId() {
        return ID;
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return "Hard Read Widget";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        // 这里可以控制组件是否显示，比如根据项目类型或配置
        return true;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return HardReadWidget.create();
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        // 框架会自动调用dispose，无需手动处理
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}