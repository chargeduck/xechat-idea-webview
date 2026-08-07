package cn.xeblog.plugin.action.handler.command;

import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.annotation.DoCommand;
import cn.xeblog.plugin.enums.Command;
import cn.xeblog.plugin.util.IdeaUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author anlingyi
 * @date 2020/8/19
 */
@DoCommand(Command.HELP)
@Slf4j
public class HelpCommandHandler extends AbstractCommandHandler {

    @Override
    public void process(String[] args) {
        log.info("[HelpCommandHandler] process 开始执行");
        var sb = new StringBuilder();
        sb.append("**远程命令<br/>**");
        sb.append("| 命令 | 说明 |\n");
        sb.append("|------|------|\n");
        for (Command command : Command.values()) {
            sb.append("| **").append(command.getCommand())
                    .append("** | ").append(command.getDesc()).append(" |\n");
        }
        sb.append("\n> *Tips*: `{ }` 必填参数，`[ ]` 可选参数，参数以空格分隔。\n");
        sb.append("\n**Version** ").append(IdeaUtils.getPluginVersion()).append("\n\n");
        sb.append("[项目地址](https://github.com/anlingyi/xechat-idea)  | [更多信息](https://xeblog.cn/?tag=xechat-idea)\n");

        ConsoleAction.atomicExec(() -> {
            ConsoleAction.renderText(sb.toString());
        });
        log.info("[HelpCommandHandler] process 执行完毕, buffer size=" + ConsoleAction.getBufferSize());
    }

    @Override
    protected boolean check(String[] args) {
        return true;
    }
}
