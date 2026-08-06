package cn.xeblog.plugin.action.handler.command;

import cn.hutool.core.util.StrUtil;
import cn.xeblog.commons.util.ParamsUtils;
import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.annotation.DoCommand;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.entity.Mask;
import cn.xeblog.plugin.enums.Command;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author anlingyi
 * @date 2020/8/19
 */
@DoCommand(Command.MASK)
public class MaskCommandHandler extends AbstractCommandHandler {

    @Getter
    @AllArgsConstructor
    private enum Config {
        /**
         * 服务器地址
         */
        USERNAME("-u"),
        /**
         * 端口
         */
        IP("-i"),
        /**
         * 清除缓存的服务器配置信息
         */
        REGION("-r"),
        /**
         *
         */
        CLEAN("-c"),
        /**
         *
         */
        NOT_SHOW("-n");

        private String key;

        public static Config getConfig(String name) {
            for (Config value : values()) {
                if (value.getKey().equals(name)) {
                    return value;
                }
            }

            return null;
        }
    }

    @Override
    public void process(String[] args) {
        Mask mask = DataCache.mask;
        if (ParamsUtils.hasKey(args, Config.CLEAN.getKey())) {
            mask.clear();
            ConsoleAction.showSimpleMsg("已解除所有屏蔽");
            return;
        }
        String usernameStr = ParamsUtils.getValue(args, Config.USERNAME.getKey());
        String regionStr = ParamsUtils.getValue(args, Config.REGION.getKey());
        String ipStr = ParamsUtils.getValue(args, Config.IP.getKey());
        if (StrUtil.isNotBlank(usernameStr)) {
            mask.getMaskUsernames().addAll(Arrays.asList(usernameStr.split(",")));
            ConsoleAction.showSimpleMsg("已模糊屏蔽用户发言:" + usernameStr);
        }
        if (StrUtil.isNotBlank(regionStr)) {
            mask.getMaskRegions().addAll(Arrays.asList(regionStr.split(",")));
            ConsoleAction.showSimpleMsg("已屏蔽省份发言:" + regionStr);
        }
        if (StrUtil.isNotBlank(ipStr)){
            mask.getMaskIps().addAll(Arrays.asList(ipStr.split(",")));
            ConsoleAction.showSimpleMsg("已屏蔽IP发言:"+ ipStr);
        }
        mask.setNotShow(ParamsUtils.hasKey(args, Config.NOT_SHOW.getKey()));
    }

}
