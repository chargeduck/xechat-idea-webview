package cn.xeblog.plugin.game;

import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.entity.game.GameDTO;
import cn.xeblog.commons.entity.game.GameRoom;

/**
 * WebView 架构下的游戏抽象。
 * 游戏 UI 已全部迁移到前端 Canvas 绘制，Java 端不再持有游戏 UI 逻辑。
 * 本类作为桥接层：将服务器下行游戏数据转发到前端，前端通过 JSBridge 上行操作。
 *
 * @author anlingyi
 */
public abstract class AbstractGame {

    /**
     * 处理服务器下发的游戏数据，子类可按需覆写以自定义转发逻辑。
     * 默认实现不做任何事，由 GameAction 负责统一转发到前端。
     */
    public void handle(GameDTO body) {
    }

    /**
     * 游戏结束清理。
     */
    public void over() {
    }

    // ==================== 房间生命周期事件 ====================

    public void roomCreated(GameRoom gameRoom) {
    }

    public void roomOpened(GameRoom gameRoom) {
    }

    public void roomClosed() {
    }

    public void playerJoined(User player) {
    }

    public void playerLeft(User player) {
    }

    public void playerReadied(User player) {
    }

    public void playerInviteFailed(User player) {
    }

    public void playerGameStarted(User user) {
    }

    public void gameStarted(GameRoom gameRoom) {
    }

    public void gameEnded() {
    }
}
