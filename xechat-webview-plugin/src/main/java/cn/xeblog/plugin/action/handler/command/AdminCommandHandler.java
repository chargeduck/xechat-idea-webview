package cn.xeblog.plugin.action.handler.command;

import cn.xeblog.commons.entity.User;
import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.action.GameAction;
import cn.xeblog.plugin.action.ToolAction;
import cn.xeblog.plugin.annotation.DoCommand;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.enums.Command;

/**
 * WebView 版管理员命令处理器。
 *
 * @author anlingyi
 */
@DoCommand(Command.ADMIN)
public class AdminCommandHandler extends AbstractCommandHandler {

    @Override
    protected void process(String[] args) {
        User user = DataCache.getCurrentUser();
        if (!user.isAdmin()) {
            ConsoleAction.showSimpleMsg("没有权限！");
            return;
        }

        if (GameAction.playing()) {
            ConsoleAction.showSimpleMsg("请先结束当前游戏！");
            return;
        }

        if (ToolAction.isOpen()) {
            ConsoleAction.showSimpleMsg("请先关闭当前打开的工具！");
            return;
        }

        // WebView 模式：通过 JSBridge 打开管理员面板
        ConsoleAction.showSimpleMsg("管理员面板功能已移至前端 WebView 页面。");
    }
}
