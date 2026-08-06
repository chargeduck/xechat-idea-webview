package cn.xeblog.plugin.action;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.entity.UserMsgDTO;
import cn.xeblog.commons.enums.Action;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.enums.Command;
import cn.xeblog.plugin.listener.MainWindowInitializedEventListener;
import cn.xeblog.plugin.util.CommandHistoryUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * WebView 版输入操作类。
 * 通过 JSBridge 接收前端输入，发送消息到服务端。
 *
 * @author anlingyi
 */
public class InputAction implements MainWindowInitializedEventListener {

    /** 冻结时间（ms） */
    private static final long FREEZE_TIME = 15 * 1000;
    /** 间隔时间（ms） */
    private static final long INTERVAL_TIME = 10 * 1000;

    private static long freezeEndTime;
    private static int sendCounter = -1;
    private static long sendCounterStartTime;

    @Override
    public void afterInit() {
        // WebView 模式下无需绑定 Swing KeyListener
    }

    /**
     * 由前端 JSBridge 调用，发送聊天消息或命令。
     *
     * @param content 用户输入内容
     */
    public static void handleInput(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }

        if (content.length() > 200) {
            ConsoleAction.showSimpleMsg("发送的内容长度不能超过200字符！");
            ConsoleAction.gotoConsoleLow();
            return;
        }

        if (content.startsWith(Command.COMMAND_PREFIX)) {
            ConsoleAction.showSimpleMsg(content);
            Command.handle(content);
        } else {
            if (!DataCache.isOnline) {
                ConsoleAction.showLoginMsg();
                ConsoleAction.gotoConsoleLow();
                return;
            }

            // 频率控制
            if (sendCounter == 0 && System.currentTimeMillis() - sendCounterStartTime < INTERVAL_TIME) {
                sendCounterStartTime = 0;
                freezeEndTime = System.currentTimeMillis() + FREEZE_TIME;
            }

            long endTime = freezeEndTime - System.currentTimeMillis();
            if (endTime > 0) {
                ConsoleAction.showSimpleMsg("消息发送过于频繁，请于" + endTime / 1000 + "s后再发...");
                ConsoleAction.gotoConsoleLow();
                return;
            }

            String[] toUsers = null;
            List<String> toUserList = ReUtil.findAll("(@)([^\\s]+)([\\s]*)", content, 2);
            if (CollectionUtil.isNotEmpty(toUserList)) {
                List<String> removeList = new ArrayList<>();
                for (String toUser : toUserList) {
                    if (DataCache.getUser(toUser) == null) {
                        removeList.add(toUser);
                    }
                }
                if (!removeList.isEmpty()) {
                    toUserList.removeAll(removeList);
                }
                if (!toUserList.isEmpty()) {
                    toUserList.add(DataCache.username);
                    toUsers = ArrayUtil.toArray(new HashSet<>(toUserList), String.class);
                }
            }

            if (sendCounter == -1) {
                sendCounter = 0;
            }
            if (++sendCounter >= 6) {
                sendCounter = 0;
            }
            if (sendCounter == 1) {
                sendCounterStartTime = System.currentTimeMillis();
            }

            CommandHistoryUtils.addCommand(content);
            MessageAction.send(new UserMsgDTO(content, toUsers), Action.CHAT);
        }

        ConsoleAction.gotoConsoleLow();
    }

}
