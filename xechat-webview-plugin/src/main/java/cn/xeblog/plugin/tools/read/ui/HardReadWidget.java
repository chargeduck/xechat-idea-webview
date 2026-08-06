package cn.xeblog.plugin.tools.read.ui;

import cn.xeblog.plugin.cache.DataCache;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * 状态栏"困难阅读"文本组件。
 * 在 IDEA 底部状态栏显示阅读进度/行号文本。
 *
 * @author LYF
 * @date 2022-07-27
 */
public class HardReadWidget implements StatusBarWidget.TextPresentation, StatusBarWidget, Disposable {

    private static final String ID = HardReadWidget.class.getName();
    private StatusBar myStatusBar;
    private String line = "";
    private boolean isInstalled = false;

    public static HardReadWidget create() {
        return new HardReadWidget();
    }

    @Override
    public @NonNls @NotNull String ID() {
        return ID;
    }

    @Override
    public @Nullable TextPresentation getPresentation() {
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        myStatusBar = statusBar;
        if (DataCache.project != null) {
            Disposer.register(DataCache.project, this);
        }
    }

    @Override
    public void dispose() {
        if (isInstalled()) {
            line = "";
            myStatusBar = null;
        }
    }

    public boolean isInstalled() {
        return line != null && myStatusBar != null;
    }

    @Override
    public @NotNull String getText() {
        return line;
    }

    public void setText(String txt) {
        line = txt;
        if (myStatusBar != null) {
            myStatusBar.updateWidget(ID);
        }
    }

    @Override
    public float getAlignment() {
        return Component.LEFT_ALIGNMENT;
    }

    @Override
    public @Nullable String getTooltipText() {
        return null;
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return null;
    }

    public void installToStatusBar() {
        Project project = DataCache.project;
        if (project == null || project.isDisposed() || isInstalled) {
            return;
        }
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar == null) {
            return;
        }
        statusBar.addWidget(this);
        Disposer.register(project, this);
        this.myStatusBar = statusBar;
        this.isInstalled = true;
    }
}
