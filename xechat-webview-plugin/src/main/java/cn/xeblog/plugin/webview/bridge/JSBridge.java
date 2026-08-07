package cn.xeblog.plugin.webview.bridge;

import cn.xeblog.plugin.action.*;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.enums.Command;
import cn.xeblog.plugin.persistence.PersistenceData;
import cn.xeblog.plugin.persistence.PersistenceService;
import cn.xeblog.plugin.tools.Tools;
import cn.xeblog.commons.enums.Action;
import cn.xeblog.commons.entity.game.GameRoomMsgDTO;
import cn.xeblog.plugin.webview.WebViewPanel;
import cn.xeblog.plugin.webview.VideoPlayerPanel;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Java ↔ JavaScript 双向通信桥（JCEF 版）。
 * JS→Java 通过 JBCefJSQuery（cefQuery）通信，
 * Java→JS 通过 executeJavaScript + CustomEvent 推送。
 *
 * @author anlingyi
 */
@Slf4j
public class JSBridge {

    private final WebViewPanel panel;
    private final Gson gson = new Gson();

    private ScheduledExecutorService messagePoller;
    private volatile boolean registered = false;

    // 缓存给前端同步读取的数据
    private String cachedState;
    private String cachedConfig;
    private String cachedTools;
    private String cachedGames;
    private String cachedReadConfig;
    private boolean cachedIsPlaying;

    public JSBridge(WebViewPanel panel) {
        this.panel = panel;
    }

    public boolean isRegistered() {
        return registered;
    }

    /**
     * 在 loadURL() 之前调用，注册 CefMessageRouter 到 CEF 客户端。
     * 必须在渲染进程启动前注册，window.cefQuery 才会在页面加载时自动就绪。
     */
    public void setupMessageRouter() {
        var routerConfig = new CefMessageRouter.CefMessageRouterConfig("cefQuery", "cefQueryCancel");
        var messageRouter = CefMessageRouter.create(routerConfig);
        messageRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId,
                                   String request, boolean persistent, CefQueryCallback callback) {
                handleQuery(request, callback);
                return true;
            }
        }, true);

        panel.getBrowser().getJBCefClient().getCefClient().addMessageRouter(messageRouter);
        log.info("[JSBridge] CefMessageRouter 已注册（loadURL 前）");
    }

    /**
     * 页面加载完成后调用，注入桥接 JS 并启动消息轮询。
     */
    public void register() {
        if (registered) return;
        try {
            log.info("[JSBridge] register() 开始");

            // 刷新缓存数据
            refreshCache();

            // 注入桥接 JS。CefMessageRouter 已在 loadURL 前注册，cefQuery 页面加载即就绪。
            var bridgeJs = buildInjectionJS();
            panel.executeJS(bridgeJs);
            log.info("[JSBridge] 桥接 JS 已注入");

            registered = true;
            startMessagePoller();
            log.info("[JSBridge] 消息轮询已启动，等待前端 ready 信号后触发 help");

        } catch (Exception e) {
            log.error("JS Bridge 注册失败", e);
        }
    }

    /**
     * 刷新缓存的同步数据
     */
    private void refreshCache() {
        cachedState = gson.toJson(new StateInfo(
                DataCache.isOnline,
                DataCache.username,
                ToolAction.isOpen(),
                ToolAction.getTools() != null ? ToolAction.getTools().getName() : null,
                DataCache.getOnlineUserTotal()
        ));
        cachedConfig = gson.toJson(PersistenceService.getData());
        cachedTools = buildToolsJson();
        cachedGames = buildGamesJson();
        cachedReadConfig = gson.toJson(DataCache.readConfig);
        cachedIsPlaying = GameAction.playing();
    }

    /**
     * 构建注入到页面的 JS 代码。
     * window.xechat API 保持与旧 JxBrowser @JsAccessible 版本完全一致。
     */
    private String buildInjectionJS() {
        // 安全转义的 JSON 数据
        var stateJson = escapeForJS(cachedState);
        var configJson = escapeForJS(cachedConfig);
        var toolsJson = escapeForJS(cachedTools);
        var gamesJson = escapeForJS(cachedGames);
        var readJson = escapeForJS(cachedReadConfig);
        var isPlaying = String.valueOf(cachedIsPlaying);

        return /* @formatter:off */
            """
            (function(){
                var __state=%s;
                var __config=%s;
                var __tools=%s;
                var __games=%s;
                var __readConfig=%s;
                var __isPlaying=%s;

                var __pendingCalls=[];
                function _tryFlush(){
                    if(typeof window.cefQuery!=='function'){setTimeout(_tryFlush,50);return;}
                    var pending=__pendingCalls.splice(0);
                    for(var i=0;i<pending.length;i++){
                        var pc=pending[i];
                        _makeCall(pc.m,pc.a,pc.cb);
                    }
                }
                function _makeCall(m,a,cb){
                    var r=JSON.stringify({method:m,args:a||[]});
                    window.cefQuery({request:r,
                        onSuccess:function(resp){
                            try{var d=JSON.parse(resp);
                            if(d&&d.event){
                                window.dispatchEvent(new CustomEvent('xechat:'+d.event,{detail:d.data||{}}));
                            }
                            if(cb)cb(resp);
                            }catch(ee){}}
                        ,
                        onFailure:function(){}
                    });
                }
                function _call(m,a,cb){
                    if(typeof window.cefQuery!=='function'){
                        console.error('[JSBridge] cefQuery 未就绪，调用入队: '+m);
                        __pendingCalls.push({m:m,a:a,cb:cb});
                        if(__pendingCalls.length===1){setTimeout(_tryFlush,50);}
                        return;
                    }
                    _makeCall(m,a,cb);
                }

                var _x=window.xechat||{};
                _x.sendMessage=function(c){_call('sendMessage',[c]);};
                _x.execCommand=function(c){_call('execCommand',[c]);};
                _x.openTool=function(i){_call('openTool',[i]);};
                _x.closeTool=function(){_call('closeTool');};
                _x.getState=function(){return JSON.stringify(__state);};
                _x.getConfig=function(){return JSON.stringify(__config);};
                _x.setConfig=function(k,v){_call('setConfig',[k,v]);};
                _x.getTools=function(){return JSON.stringify(__tools);};
                _x.getUserAgents=function(){return JSON.stringify(['PC','Android','iPhone','iPad','Mac']);};
                _x.getGameList=function(){return JSON.stringify(__games);};
                _x.joinGame=function(i){_call('joinGame',[i]);};
                _x.gameAction=function(a){_call('gameAction',[a]);};
                _x.exitGame=function(){_call('exitGame');};
                _x.isPlaying=function(){return __isPlaying;};
                _x.roomReady=function(){_call('roomReady');};
                _x.roomUnready=function(){_call('roomUnready');};
                _x.roomInvite=function(u){_call('roomInvite',[u]);};
                _x.roomLeave=function(){_call('roomLeave');};
                _x.openBrowser=function(u){_call('openBrowser',[u]);};
                _x.getReadConfig=function(){return JSON.stringify(__readConfig);};
                _x.setReadConfig=function(c){_call('setReadConfig',[c]);};
                _x.ready=function(){_call('ready');};
                _x._updateCache=function(data){
                    if(data.state)__state=data.state;
                    if(data.config)__config=data.config;
                    if(data.tools)__tools=data.tools;
                    if(data.games)__games=data.games;
                    if(data.readConfig!==undefined)__readConfig=data.readConfig;
                    if(data.isPlaying!==undefined)__isPlaying=data.isPlaying;
                };
                window.xechat=_x;
                console.log('[JSBridge] 注入完成, cefQuery='+(typeof window.cefQuery)+', xechat keys='+Object.keys(_x).join(','));
            })();
            """.formatted(stateJson, configJson, toolsJson, gamesJson, readJson, isPlaying);
            /* @formatter:on */
    }

    /**
     * 处理前端通过 cefQuery 发来的调用
     */
    private void handleQuery(String data, CefQueryCallback callback) {
        try {
            var call = gson.fromJson(data, JsCall.class);
            var method = call.method;
            var args = call.args != null ? call.args : List.<String>of();

            CompletableFuture.runAsync(() -> {
                try {
                    dispatchJsCall(method, args);
                } catch (Exception e) {
                    log.error("处理 JS 调用失败: {}", method, e);
                }
            });

            callback.success("ok");

        } catch (Exception e) {
            log.error("解析 JS 调用失败", e);
            callback.failure(400, "Invalid request: " + e.getMessage());
        }
    }

    /**
     * 分发 JS 调用到具体处理器
     */
    private void dispatchJsCall(String method, List<String> args) {
        switch (method) {
            case "sendMessage":
                if (!args.isEmpty()) InputAction.handleInput(args.get(0));
                break;
            case "execCommand":
                if (!args.isEmpty()) Command.handle(args.get(0));
                break;
            case "openTool":
                if (!args.isEmpty()) openToolInternal(Integer.parseInt(args.get(0)));
                break;
            case "closeTool":
                ToolAction.over();
                pushEvent("toolClose", Collections.emptyMap());
                break;
            case "setConfig":
                if (args.size() >= 2) setConfigInternal(args.get(0), args.get(1));
                break;
            case "joinGame":
                if (!args.isEmpty()) joinGameInternal(Integer.parseInt(args.get(0)));
                break;
            case "gameAction":
                if (!args.isEmpty()) GameAction.handleInput(args.get(0));
                break;
            case "exitGame":
                GameAction.over();
                pushEvent("gameOver", Collections.emptyMap());
                break;
            case "roomReady":
                roomReadyInternal();
                break;
            case "roomUnready":
                roomUnreadyInternal();
                break;
            case "roomInvite":
                if (!args.isEmpty()) roomInviteInternal(args.get(0));
                break;
            case "roomLeave":
                roomLeaveInternal();
                break;
            case "openBrowser":
                if (!args.isEmpty()) ToolAction.openBrowser(args.get(0));
                break;
            case "setReadConfig":
                if (!args.isEmpty()) setReadConfigInternal(args.get(0));
                break;
            case "ready":
                onFrontReady();
                break;
        }
    }

    /**
     * 前端 Vue 应用挂载、监听器全部注册完毕后回调。
     * 此处才安全地触发 #help 命令，避免消息早于监听器注册到达而丢失。
     */
    private void onFrontReady() {
        log.info("[JSBridge] 收到前端 ready 信号，触发 help 命令");
        try {
            cn.xeblog.plugin.enums.Command.handle("#help");
            log.info("[JSBridge] help 命令已执行");
        } catch (Exception e) {
            log.error("触发 help 命令失败", e);
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 打开工具。index=2（BROWSER2）需走 JxBrowser 视频播放通道。
     */
    private void openToolInternal(int index) {
        var tool = Tools.getTool(index);
        if (tool == null) {
            pushMessage("没有找到该工具");
            return;
        }
        if (tool.isRequiredLogin() && !DataCache.isOnline) {
            pushMessage("请先登录");
            return;
        }

        // BROWSER2（index=2）使用 JxBrowser 独立面板（支持 H.264 视频解码）
        if (tool == Tools.BROWSER2) {
            VideoPlayerPanel.getInstance().open();
            return;
        }

        ToolAction.create(tool);
        pushEvent("toolOpen", Map.of("index", index, "name", tool.getName()));
    }

    private void joinGameInternal(int gameIndex) {
        try {
            GameAction.create(gameIndex);
            pushEvent("gameStart", Map.of("gameIndex", gameIndex));
        } catch (Exception e) {
            log.error("加入游戏失败", e);
        }
    }

    private void setConfigInternal(String key, String value) {
        var data = PersistenceService.getData();
        switch (key) {
            case "username":
                data.setUsername(value);
                DataCache.username = value;
                break;
            case "jxBrowserLicense":
                data.setJxBrowserLicense(value);
                break;
            case "msgNotify":
                data.setMsgNotify(Integer.parseInt(value));
                DataCache.msgNotify = Integer.parseInt(value);
                break;
            default:
                log.warn("未知配置项: {}", key);
        }
    }

    private void roomReadyInternal() {
        try {
            var dto = new GameRoomMsgDTO();
            dto.setRoomId(GameAction.getRoomId());
            dto.setMsgType(GameRoomMsgDTO.MsgType.PLAYER_READY);
            MessageAction.send(dto, Action.GAME_ROOM);
        } catch (Exception e) {
            log.error("准备失败", e);
        }
    }

    private void roomUnreadyInternal() {
        try {
            var dto = new cn.xeblog.commons.entity.game.GameRoomMsgDTO();
            dto.setRoomId(GameAction.getRoomId());
            dto.setMsgType(GameRoomMsgDTO.MsgType.PLAYER_CANCEL_READY);
            MessageAction.send(dto, Action.GAME_ROOM);
        } catch (Exception e) {
            log.error("取消准备失败", e);
        }
    }

    private void roomInviteInternal(String username) {
        try {
            cn.xeblog.commons.entity.game.GameRoomMsgDTO dto =
                    new cn.xeblog.commons.entity.game.GameRoomMsgDTO();
            dto.setRoomId(GameAction.getRoomId());
            dto.setMsgType(cn.xeblog.commons.entity.game.GameRoomMsgDTO.MsgType.PLAYER_INVITE);
            dto.setContent(username);
            MessageAction.send(dto, Action.GAME_ROOM);
        } catch (Exception e) {
            log.error("邀请失败", e);
        }
    }

    private void roomLeaveInternal() {
        try {
            var dto = new cn.xeblog.commons.entity.game.GameRoomMsgDTO();
            dto.setRoomId(GameAction.getRoomId());
            dto.setMsgType(cn.xeblog.commons.entity.game.GameRoomMsgDTO.MsgType.PLAYER_LEFT);
            MessageAction.send(dto, Action.GAME_ROOM);
            GameAction.over();
            pushEvent("gameOver", Collections.emptyMap());
        } catch (Exception e) {
            log.error("离开房间失败", e);
        }
    }

    private void setReadConfigInternal(String configJson) {
        try {
            var config = gson.fromJson(configJson, cn.xeblog.plugin.tools.read.ReadConfig.class);
            if (config != null) {
                DataCache.readConfig = config;
            }
        } catch (Exception e) {
            log.error("更新阅读配置失败", e);
        }
    }

    // ==================== 推送 API（Java → JS） ====================

    /**
     * 推送事件到前端，同时更新前端缓存。
     */
    public void pushEvent(String event, Object data) {
        refreshCache();
        var json = data != null ? gson.toJson(data) : "{}";
        var js = "window.dispatchEvent(new CustomEvent('xechat:%s', { detail: %s }))".formatted(event, json);
        log.info("[JSBridge] pushEvent: event={}, dataLength={}", event, json.length());
        panel.executeJS(js);
        log.info("[JSBridge] executeJS 已调用");

        // 推送最新缓存同步
        syncCache();
    }

    /**
     * 推送消息文本
     */
    public void pushMessage(String message) {
        pushEvent("message", Collections.singletonMap("text", message));
    }

    /**
     * 将最新缓存同步到前端
     */
    private void syncCache() {
        refreshCache();
        var cacheJs = "window.xechat&&window.xechat._updateCache({state:%s,config:%s,tools:%s,games:%s,readConfig:%s,isPlaying:%s})"
                .formatted(cachedState, cachedConfig, cachedTools, cachedGames, cachedReadConfig,
                        String.valueOf(cachedIsPlaying));
        panel.executeJS(cacheJs);
    }

    // ==================== 控制台消息轮询 ====================

    private void startMessagePoller() {
        if (messagePoller != null) return;
        log.info("[JSBridge] 启动消息轮询器（200ms 间隔）");
        messagePoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "xechat-msg-poller");
            t.setDaemon(true);
            return t;
        });
        messagePoller.scheduleAtFixedRate(() -> {
            try {
                var messages = ConsoleAction.drainMessages();
                if (!messages.isEmpty()) {
                    log.info("[JSBridge] 轮询到 {} 条消息，准备推送\n{}", messages.size(),messages);
                    pushEvent("console", messages);
                    log.info("[JSBridge] console 事件已推送");
                }
            } catch (Exception e) {
                log.error("[JSBridge] 轮询异常", e);
            }
        }, 200, 200, TimeUnit.MILLISECONDS);
    }

    public void dispose() {
        if (messagePoller != null) {
            messagePoller.shutdownNow();
            messagePoller = null;
        }
        registered = false;
    }

    // ==================== 辅助方法 ====================

    private String buildToolsJson() {
        var tools = Tools.values();
        var infos = new ToolInfo[tools.length];
        for (int i = 0; i < tools.length; i++) {
            var t = tools[i];
            infos[i] = new ToolInfo(i, t.getName(), t.isRequiredLogin());
        }
        return gson.toJson(infos);
    }

    private String buildGamesJson() {
        cn.xeblog.commons.enums.Game[] games = cn.xeblog.commons.enums.Game.values();
        GameInfo[] infos = new GameInfo[games.length];
        for (int i = 0; i < games.length; i++) {
            cn.xeblog.commons.enums.Game g = games[i];
            infos[i] = new GameInfo(i, g.getName(), false);
        }
        return gson.toJson(infos);
    }

    private String escapeForJS(String json) {
        return json
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // ==================== DTO ====================

    @Data
    @AllArgsConstructor
    public static class StateInfo {
        private boolean online;
        private String username;
        private boolean toolOpen;
        private String currentTool;
        private int onlineCount;
    }

    @Data
    @AllArgsConstructor
    public static class ToolInfo {
        private int index;
        private String name;
        private boolean requireLogin;
    }

    @Data
    @AllArgsConstructor
    public static class GameInfo {
        private int index;
        private String name;
        private boolean requireLogin;
    }

    static class JsCall {
        String method;
        List<String> args;
    }
}
