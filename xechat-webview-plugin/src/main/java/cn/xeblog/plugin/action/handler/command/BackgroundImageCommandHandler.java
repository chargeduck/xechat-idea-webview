package cn.xeblog.plugin.action.handler.command;

import cn.xeblog.plugin.annotation.DoCommand;
import cn.xeblog.plugin.enums.Command;
import cn.xeblog.plugin.action.ConsoleAction;

/**
 * WebView 版背景图命令处理器。
 *
 * @author anlingyi
 */
@DoCommand(Command.BACKGROUND_IMAGE)
public class BackgroundImageCommandHandler extends AbstractCommandHandler {

    @Override
    protected void process(String[] args) {
        // WebView 模式：通过 JSBridge 打开背景图设置页面
        ConsoleAction.showSimpleMsg("背景图设置功能已移至前端 WebView 页面。");
    }

    @Override
    protected boolean check(String[] args) {
        return true;
    }
}
