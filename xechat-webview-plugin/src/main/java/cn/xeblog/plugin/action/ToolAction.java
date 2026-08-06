package cn.xeblog.plugin.action;

import cn.xeblog.plugin.factory.ToolsFactory;
import cn.xeblog.plugin.tools.AbstractTool;
import cn.xeblog.plugin.tools.Tools;

/**
 * @author anlingyi
 * @date 2022/8/5 5:37 上午
 */
public class ToolAction {

    /**
     * 当前打开的工具
     */
    private static Tools tools;

    /**
     * 工具动作
     */
    private static AbstractTool action;

    public static Tools getTools() {
        return tools;
    }

    public static AbstractTool create(Tools tools) {
        ToolAction.tools = tools;
        action = ToolsFactory.produce(tools);
        return action;
    }

    public static boolean isOpen() {
        return action != null;
    }

    public static void over() {
        if (isOpen()) {
            action.over();
        }

        clean();
    }

    public static void clean() {
        tools = null;
        action = null;
    }

    /**
     * 由前端 JSBridge 调用，通过浏览器工具打开指定 URL
     */
    public static void openBrowser(String url) {
        Tools browserTool = Tools.BROWSER;
        create(browserTool);
        if (action != null) {
            action.init();
        }
    }

}
