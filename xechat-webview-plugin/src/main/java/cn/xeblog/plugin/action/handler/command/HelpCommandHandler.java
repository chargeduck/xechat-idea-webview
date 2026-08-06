package cn.xeblog.plugin.action.handler.command;

import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.annotation.DoCommand;
import cn.xeblog.plugin.enums.Command;
import cn.xeblog.plugin.util.IdeaUtils;

/**
 * @author anlingyi
 * @date 2020/8/19
 */
@DoCommand(Command.HELP)
public class HelpCommandHandler extends AbstractCommandHandler {

    @Override
    public void process(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (Command command : Command.values()) {
            sb.append("· ").append(command.getCommand())
                    .append("：").append(command.getDesc()).append("\n");
        }
        sb.append(" > Tips: \"{ }\"表示输入参数占位符，\"[ ]\"内的参数为可选参数，所有参数均以空格分隔。\n");
        sb.append("\n Version ").append(IdeaUtils.getPluginVersion()).append("\n");

        ConsoleAction.atomicExec(() -> {
            ConsoleAction.showSimpleMsg(" 命令列表 & 触发命令前缀 " + Command.COMMAND_PREFIX);
            ConsoleAction.renderText(sb.toString());
            ConsoleAction.renderText("误入充电鸭鱼塘 怎么切换？\n");
            ConsoleAction.renderText("#exit 退出\n");
            ConsoleAction.renderText("#showServer 查询服务列表\n");
            ConsoleAction.renderText("#login {昵称} -s 0\n");
            ConsoleAction.renderText(" --------------\n ");
            ConsoleAction.renderUrl("[开源]", "https://github.com/anlingyi/xechat-idea");
            ConsoleAction.renderText("  ");
            ConsoleAction.renderUrl("[更多]", "https://xeblog.cn/?tag=xechat-idea");
            ConsoleAction.renderText("\n --------------\n");
        });
    }

    @Override
    protected boolean check(String[] args) {
        return true;
    }
}
