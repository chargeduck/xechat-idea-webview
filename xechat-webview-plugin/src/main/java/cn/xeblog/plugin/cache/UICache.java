package cn.xeblog.plugin.cache;

/**
 * UI 缓存（Vue 迁移版）。
 * 原先持有 AbstractPanelComponent 引用（Swing 面板），
 * 迁移后改为纯 POJO，存储当前激活的工具标识。
 *
 * @author anlingyi
 * @date 2023/2/18 9:24 PM
 */
public class UICache {

    /**
     * 当前激活的工具 ID（对应 Tools 枚举名），供 JSBridge 查询
     */
    public static String currentToolId;

    /**
     * 工具面板是否可见
     */
    public static boolean toolPanelVisible;

}
