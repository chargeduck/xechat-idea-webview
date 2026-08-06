package cn.xeblog.plugin.tools;

/**
 * 抽象工具基类（Vue 迁移版）。
 * 原先继承 AbstractPanelComponent（Swing JPanel），
 * 迁移后改为纯逻辑抽象类，不依赖任何 Swing/AWT。
 *
 * @author anlingyi
 * @date 2022/8/5 5:19 上午
 */
public abstract class AbstractTool {

    /**
     * 工具结束时调用，用于释放资源
     */
    public void over() {
    }

    /**
     * 工具初始化，子类可重写
     */
    public void init() {
    }
}
