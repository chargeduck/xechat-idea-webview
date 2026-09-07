package cn.xeblog.server.action.handler.react;

import cn.hutool.core.util.StrUtil;
import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.entity.react.React;
import cn.xeblog.commons.entity.react.request.AdminReact;
import cn.xeblog.commons.entity.react.result.AdminReactResult;
import cn.xeblog.commons.entity.react.result.ReactResult;
import cn.xeblog.commons.enums.MessageType;
import cn.xeblog.commons.util.ParamsUtils;
import cn.xeblog.server.action.ChannelAction;
import cn.xeblog.server.annotation.DoReact;
import cn.xeblog.server.builder.ResponseBuilder;
import cn.xeblog.server.cache.UserCache;
import cn.xeblog.server.config.GlobalConfig;
import cn.xeblog.server.config.ServerConfig;
import cn.xeblog.server.forward.client.ForwardClient;

import java.util.Arrays;

/**
 * @author anlingyi
 * @date 2023/2/18 8:16 PM
 */
@DoReact(React.ADMIN)
public class AdminReactHandler extends AbstractReactHandler<AdminReact, AdminReactResult> {

    @Override
    protected void process(User user, AdminReact body, ReactResult<AdminReactResult> result) {
        if (!user.isAdmin()) {
            result.setMsg("没有权限！");
            return;
        }

        String msg = null;
        switch (body.getOperate()) {
            case QUERY_PERMIT:
                break;
            case GLOBAL_MAX_FILE_SIZE:
                GlobalConfig.UPLOAD_FILE_MAX_SIZE = Integer.parseInt(body.getValue());
                ChannelAction.send(ResponseBuilder.system("管理员已将文件上传的大小限制为" + GlobalConfig.UPLOAD_FILE_MAX_SIZE + "KB!"));
                break;
            case GLOBAL_PERMIT_ADD:
                GlobalConfig.GLOBAL_PERMIT |= body.getPermissions().getValue();
                switch (body.getPermissions()) {
                    case SEND_FILE:
                        msg = "鱼塘已允许全员发送图片！";
                        break;
                    case SPEAK:
                        msg = "鱼塘已解除全员禁言！";
                        break;
                }
                break;
            case GLOBAL_PERMIT_REMOVE:
                if (body.getPermissions().hasPermit(GlobalConfig.GLOBAL_PERMIT)) {
                    GlobalConfig.GLOBAL_PERMIT ^= body.getPermissions().getValue();
                    switch (body.getPermissions()) {
                        case SEND_FILE:
                            msg = "鱼塘已禁止全员发送图片！";
                            break;
                        case SPEAK:
                            msg = "鱼塘已开启全员禁言！";
                            break;
                    }
                }
                break;
            case USER_PERMIT_ADD:
            case USER_PERMIT_REMOVE:
                User execUser = UserCache.get(body.getUid());
                if (execUser == null) {
                    result.setMsg("用户不存在！");
                    return;
                }

                if (body.getOperate() == AdminReact.Operate.USER_PERMIT_ADD) {
                    execUser.addPermit(body.getPermissions());
                    switch (body.getPermissions()) {
                        case SEND_FILE:
                            msg = "已允许[" + execUser.getUsername() + "]发送图片！";
                            break;
                        case SPEAK:
                            msg = "已允许[" + execUser.getUsername() + "]发言！";
                            break;
                    }
                } else {
                    execUser.removePermit(body.getPermissions());
                    switch (body.getPermissions()) {
                        case SEND_FILE:
                            msg = "已禁止[" + execUser.getUsername() + "]发送图片！";
                            break;
                        case SPEAK:
                            msg = "已禁止[" + execUser.getUsername() + "]发言！";
                            break;
                    }
                }

                GlobalConfig.addUserPermit(user, execUser.getPermit());
                ChannelAction.send(ResponseBuilder.build(execUser, null, MessageType.STATUS_UPDATE));
                break;
            case FORWARD:
                // 转发注册：value 为空用 yml 配置；也可传 forward -h xxx -p xxx 注册到指定 hub
                handleForward(body, result);
                break;
        }

        if (msg != null) {
            ChannelAction.send(ResponseBuilder.system(msg));
        }

        result.setSucceed(true);
        result.setData(new AdminReactResult(GlobalConfig.GLOBAL_PERMIT, GlobalConfig.UPLOAD_FILE_MAX_SIZE));
    }

    /**
     * 处理 forward 注册命令：无参用 yml 中 forward.host/forward.port；
     * 带 -h/-p 则覆盖并注册到指定 hub。注册失败计数会被重置，成功后守护线程恢复自动重连。
     */
    private void handleForward(AdminReact body, ReactResult<AdminReactResult> result) {
        ServerConfig config = ServerConfig.getConfig();
        String value = body.getValue();
        String[] args = StrUtil.isBlank(value) ? new String[0] : StrUtil.trim(value).split("\\s+");
        // 兼容带 "forward" 命令前缀的传值：forward -h xxx -p xxx
        if (args.length > 0 && StrUtil.equalsIgnoreCase(args[0], "forward")) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        String host = ParamsUtils.getValue(args, "-h");
        String portStr = ParamsUtils.getValue(args, "-p");
        if (StrUtil.isNotBlank(host)) {
            config.setForwardHost(StrUtil.trim(host));
        }
        if (StrUtil.isNotBlank(portStr)) {
            try {
                config.setForwardPort(Integer.parseInt(StrUtil.trim(portStr)));
            } catch (NumberFormatException e) {
                result.setMsg("端口格式不正确: " + portStr + "（用法: forward 或 forward -h hub地址 -p hub端口）");
                result.setSucceed(false);
                return;
            }
        }
        if (StrUtil.isBlank(config.getForwardHost())) {
            result.setMsg("未配置 forward.host 且未传入 -h，无法注册转发（用法: forward 或 forward -h hub地址 -p hub端口）");
            result.setSucceed(false);
            return;
        }

        // 接入名取 yml 配置；未配置由 hub 探测/兜底。重置失败计数并立即注册
        boolean ok = ForwardClient.resetAndRetry();
        ChannelAction.send(ResponseBuilder.system(ok
                ? StrUtil.format("forward 注册成功：{}:{}", config.getForwardHost(), config.getForwardPort())
                : StrUtil.format("forward 注册失败：{}:{}（可稍后重试该命令）", config.getForwardHost(), config.getForwardPort())));
    }

}
