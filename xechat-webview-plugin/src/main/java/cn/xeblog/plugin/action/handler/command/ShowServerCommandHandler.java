package cn.xeblog.plugin.action.handler.command;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.xeblog.commons.entity.OnlineServer;
import cn.xeblog.commons.util.ParamsUtils;
import cn.xeblog.commons.util.ServerUtils;
import cn.xeblog.plugin.action.ConnectionAction;
import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.annotation.DoCommand;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.enums.Command;
import cn.xeblog.plugin.util.MdTableUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author anlingyi
 * @date 2020/9/11
 */
@Slf4j
@DoCommand(Command.SHOW_SERVER)
public class ShowServerCommandHandler extends AbstractCommandHandler {

    @Getter
    @AllArgsConstructor
    private enum Config {
        /**
         * 清理缓存
         */
        CLEAN("-c");

        private String key;
    }

    @Override
    public void process(String[] args) {
        // 存在-c参数，清除缓存
        if (ParamsUtils.hasKey(args, Config.CLEAN.getKey())) {
            DataCache.serverList = null;
        }

        if (CollUtil.isEmpty(DataCache.serverList)) {
            // 查询服务列表,并缓存
            ConsoleAction.showSimpleMsg("正在更新鱼塘列表...");
            GlobalThreadPool.execute(() -> {
                try {
                    DataCache.serverList = ServerUtils.getServerList();
                    String url = "https://gitee.com/chargeduck/xechat-idea/raw/main/server_list.json";
                    String resp = HttpRequest.get(url)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                            .execute()
                            .body();
                    log.info("server list resp: {}", resp);
                    log.info("server list:{}", DataCache.serverList);
                    showServerList();
                } catch (Exception e) {
                    e.printStackTrace();
                    ConsoleAction.showSimpleMsg("鱼塘列表更新异常!");
                }
            });
        } else {
            showServerList();
        }
    }

    private void showServerList() {
        List<OnlineServer> serverList = DataCache.serverList;
        if (CollUtil.isEmpty(serverList)) {
            ConsoleAction.showSimpleMsg("没有鱼塘！");
            return;
        }

        OnlineServer currentServer = null;
        ConnectionAction connection = DataCache.connectionAction;
        if (DataCache.isOnline && connection != null) {
            currentServer = new OnlineServer();
            currentServer.setIp(connection.getHost());
            currentServer.setPort(connection.getPort());
        }

        var mdTable = MdTableUtil.create();
        mdTable.addHeader("编号", "鱼塘", "状态");
        for (int i = 0; i < serverList.size(); i++) {
            OnlineServer server = serverList.get(i);
            boolean isCurrentServer = server.equals(currentServer);
            mdTable.addBody(Integer.toString(i), server.getName(), isCurrentServer ? "已连接" : "未连接");
        }
        ConsoleAction.showSimpleMsg(mdTable.toString());
    }

    @Override
    protected boolean check(String[] args) {
        return true;
    }
}
