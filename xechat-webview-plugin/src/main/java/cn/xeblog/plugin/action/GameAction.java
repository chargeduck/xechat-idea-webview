package cn.xeblog.plugin.action;

import cn.xeblog.commons.entity.game.GameDTO;
import cn.xeblog.commons.entity.game.GameRoom;
import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.entity.Response;
import cn.xeblog.commons.entity.game.chess.ChessDTO;
import cn.xeblog.commons.entity.game.gobang.GobangDTO;
import cn.xeblog.commons.entity.game.landlords.LandlordsGameDTO;
import cn.xeblog.commons.entity.game.mahjong.MahjongGameDto;
import cn.xeblog.commons.entity.game.uno.UNOGameDto;
import cn.xeblog.commons.entity.game.zillionaire.dto.MonopolyGameDto;
import cn.xeblog.commons.enums.Action;
import cn.xeblog.commons.enums.Game;
import cn.xeblog.plugin.game.AbstractGame;
import cn.xeblog.plugin.webview.WebViewPanel;
import com.google.gson.Gson;

/**
 * WebView 架构下的游戏动作管理。
 * 游戏 UI 已迁移到前端 Canvas，Java 端仅负责状态管理与数据转发。
 *
 * @author anlingyi
 */
public class GameAction {

    /** 当前游戏 */
    private static Game game;

    /** 当前游戏桥接实例 */
    private static AbstractGame action;

    /** 我的昵称 */
    private static String nickname;

    /** 邀请人昵称 */
    private static String inviter;

    /** 当前游戏房间号 */
    private static String roomId;

    public static void setRoomId(String roomId) {
        GameAction.roomId = roomId;
    }

    public static String getRoomId() {
        return roomId;
    }

    public static boolean isOfflineGame() {
        return roomId == null;
    }

    public static String getNickname() {
        return nickname;
    }

    public static void setNickname(String nickname) {
        GameAction.nickname = nickname;
    }

    public static void setGame(Game game) {
        GameAction.game = game;
    }

    public static void setInviter(String inviter) {
        GameAction.inviter = inviter;
    }

    public static String getInviter() {
        return inviter;
    }

    public static boolean isProactive() {
        return inviter == null;
    }

    public static Game getGame() {
        return game;
    }

    public static String getName() {
        if (game == null) {
            return "";
        }
        return game.getName();
    }

    /**
     * 处理服务器下发的游戏数据，转发到前端 Canvas。
     */
    public static void handle(Response<GameDTO> response) {
        if (playing()) {
            GameDTO body = response.getBody();
            action.handle(body);
            pushToFrontend(body);
        }
    }

    public static void over() {
        if (playing()) {
            action.over();
        }
        clean();
    }

    /**
     * 由前端 JSBridge 调用，根据游戏索引创建游戏实例。
     */
    public static AbstractGame create(int gameIndex) {
        Game[] games = Game.values();
        if (gameIndex < 0 || gameIndex >= games.length) {
            return null;
        }
        setGame(games[gameIndex]);
        return create();
    }

    public static AbstractGame create() {
        if (game == null) {
            return null;
        }

        GameAction.action = new AbstractGame() {
            @Override
            public void handle(GameDTO body) {
                pushToFrontend(body);
            }

            @Override
            public void over() {
                // 前端已通过 JSBridge.exitGame 获知游戏结束
            }

            @Override
            public void roomCreated(GameRoom gameRoom) {
                pushRoomEvent("roomCreated", gameRoom);
            }

            @Override
            public void roomOpened(GameRoom gameRoom) {
                pushRoomEvent("roomOpened", gameRoom);
            }

            @Override
            public void roomClosed() {
                pushRoomEvent("roomClosed", null);
            }

            @Override
            public void playerJoined(User player) {
                pushRoomEvent("playerJoined", player);
            }

            @Override
            public void playerLeft(User player) {
                pushRoomEvent("playerLeft", player);
            }

            @Override
            public void playerReadied(User player) {
                pushRoomEvent("playerReadied", player);
            }

            @Override
            public void playerInviteFailed(User player) {
                pushRoomEvent("playerInviteFailed", player);
            }

            @Override
            public void gameStarted(GameRoom gameRoom) {
                pushRoomEvent("gameStarted", gameRoom);
            }

            @Override
            public void gameEnded() {
                pushRoomEvent("gameEnded", null);
            }
        };
        return action;
    }

    /**
     * 由前端 JSBridge 调用，将用户游戏操作上行到服务器。
     */
    public static void handleInput(String actionData) {
        if (action != null && game != null) {
            try {
                Class<? extends GameDTO> dtoClass = getDtoClass(game);
                GameDTO dto = new Gson().fromJson(actionData, dtoClass);
                if (roomId != null) {
                    dto.setRoomId(roomId);
                }
                dto.setGame(game);
                MessageAction.send(dto, Action.GAME);
            } catch (Exception e) {
                ConsoleAction.showSimpleMsg("游戏操作发送失败: " + e.getMessage());
            }
        }
    }

    /**
     * 根据游戏类型获取对应的 GameDTO 子类。
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends GameDTO> getDtoClass(Game game) {
        return switch (game) {
            case CHINESE_CHESS -> ChessDTO.class;
            case GOBANG -> GobangDTO.class;
            case LANDLORDS -> LandlordsGameDTO.class;
            case MAHJONG -> MahjongGameDto.class;
            case UNO -> UNOGameDto.class;
            case MONOPOLY -> MonopolyGameDto.class;
            default -> GameDTO.class;
        };
    }

    public static void clean() {
        game = null;
        action = null;
        inviter = null;
        nickname = null;
        roomId = null;
    }

    public static boolean playing() {
        return action != null;
    }

    public static boolean isOver() {
        return game == null;
    }

    public static AbstractGame getAction() {
        return action;
    }

    // ==================== 内部方法 ====================

    /**
     * 将游戏数据推送到前端 Canvas。
     */
    private static void pushToFrontend(GameDTO body) {
        WebViewPanel panel = WebViewPanel.getInstance();
        if (panel.isInitialized()) {
            String json = new Gson().toJson(body);
            panel.executeJS(String.format(
                    "window.dispatchEvent(new CustomEvent('xechat:gameData', { detail: %s }))",
                    json
            ));
        }
    }

    /**
     * 将房间事件推送到前端。
     */
    private static void pushRoomEvent(String type, Object data) {
        WebViewPanel panel = WebViewPanel.getInstance();
        if (panel.isInitialized()) {
            Gson gson = new Gson();
            String dataJson = data != null ? gson.toJson(data) : "null";
            String payload = String.format("{\"type\":\"%s\",\"data\":%s}", type, dataJson);
            panel.executeJS(String.format(
                    "window.dispatchEvent(new CustomEvent('xechat:gameRoom', { detail: %s }))",
                    payload
            ));
        }
    }
}
