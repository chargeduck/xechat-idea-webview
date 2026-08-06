/**
 * xechat-webview 主应用。
 * Vue 3 CDN + 简单 hash 路由，无构建工具链。
 */
(function () {
    'use strict';

    const { createApp, ref, reactive, computed, watch, onMounted, onUnmounted, nextTick, h } = Vue;

    // ======================== 全局状态 ========================
    const state = reactive({
        online: false,
        username: '',
        onlineCount: 0,
        toolOpen: false,
        currentTool: '',
        currentRoute: 'chat',
        messages: [],
        tools: [],
        games: [],
        gamePlaying: false,
        currentGameIndex: -1,
        gameStateData: null,
        settingsOpen: false,
        // 房间状态
        inRoom: false,
        roomId: '',
        roomGameName: '',
        roomGameIndex: -1,
        roomNums: 0,
        isHomeowner: false,
        roomPlayers: [],
        roomReady: false
    });

    // ======================== 路由 ========================
    function navigate(route) {
        state.currentRoute = route;
    }
    window.addEventListener('hashchange', function () {
        state.currentRoute = location.hash.slice(2) || 'chat';
    });
    state.currentRoute = location.hash.slice(2) || 'chat';

    // ======================== 消息处理 ========================
    xechat.on('console', function (msgs) {
        msgs.forEach(function (m) {
            state.messages.push({ text: m, time: new Date().toLocaleTimeString() });
        });
        // 限制消息数
        if (state.messages.length > 1000) {
            state.messages.splice(0, state.messages.length - 1000);
        }
        nextTick(function () {
            var el = document.querySelector('.message-list');
            if (el) el.scrollTop = el.scrollHeight;
        });
    });
    xechat.on('toolOpen', function () {
        state.toolOpen = true;
        refreshState();
    });
    xechat.on('toolClose', function () {
        state.toolOpen = false;
        refreshState();
    });
    xechat.on('gameStart', function (data) {
        state.gamePlaying = true;
        state.currentGameIndex = data.gameIndex;
        navigate('game');
    });
    xechat.on('gameOver', function () {
        state.gamePlaying = false;
        state.currentGameIndex = -1;
        resetRoomState();
        navigate('chat');
    });
    xechat.on('gameState', function (data) {
        state.gameStateData = data;
    });
    xechat.on('gameRoom', function (data) {
        handleRoomEvent(data);
    });
    xechat.on('message', function (data) {
        state.messages.push({ text: data.text, time: new Date().toLocaleTimeString(), type: 'system' });
        nextTick(function () {
            var el = document.querySelector('.message-list');
            if (el) el.scrollTop = el.scrollHeight;
        });
    });

    function refreshState() {
        try {
            var json = xechat.getState();
            var s = JSON.parse(json);
            state.online = s.online;
            state.username = s.username;
            state.onlineCount = s.onlineCount;
            state.toolOpen = s.toolOpen;
            state.currentTool = s.currentTool;
        } catch (e) {}
    }

    function handleRoomEvent(data) {
        var type = data.type;
        switch (type) {
            case 'roomCreated':
            case 'roomOpened':
                var room = data.data;
                state.inRoom = true;
                state.roomId = room.id;
                state.roomGameName = room.game ? room.game.name : '';
                state.roomNums = room.nums;
                state.isHomeowner = room.homeowner && room.homeowner.username === state.username;
                state.roomReady = false;
                // 初始化玩家列表
                var players = [];
                var users = room.users || {};
                Object.keys(users).forEach(function (key) {
                    var p = users[key];
                    players.push({ id: p.id, username: p.username, readied: p.readied });
                });
                state.roomPlayers = players;
                navigate('room');
                break;
            case 'roomClosed':
            case 'gameEnded':
                resetRoomState();
                navigate('chat');
                break;
            case 'playerJoined':
                var exists = state.roomPlayers.find(function (p) { return p.username === data.data.username; });
                if (!exists) {
                    state.roomPlayers.push({ id: data.data.id, username: data.data.username, readied: false });
                }
                break;
            case 'playerLeft':
                state.roomPlayers = state.roomPlayers.filter(function (p) { return p.username !== data.data.username; });
                break;
            case 'playerReadied':
                var rp = state.roomPlayers.find(function (p) { return p.username === data.data.username; });
                if (rp) { rp.readied = true; }
                if (data.data.username === state.username) { state.roomReady = true; }
                break;
            case 'playerInviteFailed':
                break;
            case 'gameStarted':
                state.gamePlaying = true;
                state.currentGameIndex = state.roomGameIndex;
                state.inRoom = false;
                navigate('game');
                break;
        }
    }

    function resetRoomState() {
        state.inRoom = false;
        state.roomId = '';
        state.roomGameName = '';
        state.roomGameIndex = -1;
        state.roomNums = 0;
        state.isHomeowner = false;
        state.roomPlayers = [];
        state.roomReady = false;
    }

    // 房间事件桥接：前端 CustomEvent → Java JSBridge
    window.addEventListener('xechat:roomReady', function () {
        xechat.roomReady();
    });
    window.addEventListener('xechat:roomUnready', function () {
        xechat.roomUnready();
    });
    window.addEventListener('xechat:roomInvite', function (e) {
        xechat.roomInvite(e.detail.username);
    });
    window.addEventListener('xechat:roomLeave', function () {
        xechat.roomLeave();
        resetRoomState();
        navigate('chat');
    });

    function loadTools() {
        try {
            var json = xechat.getTools();
            state.tools = JSON.parse(json);
        } catch (e) {}
    }

    function loadGames() {
        try {
            var json = xechat.getGameList();
            state.games = JSON.parse(json);
        } catch (e) {}
    }

    // ======================== 组件：StatusBar ========================
    const statusBar = {
        template: '<div class="status-bar">' +
            '<span><span :class="state.online ? \'online-dot\' : \'online-dot offline-dot\'"></span>' +
            '{{ state.online ? "已连接" : "未连接" }}</span>' +
            '<span v-if="state.username">{{ state.username }}</span>' +
            '<span>在线: {{ state.onlineCount }}</span>' +
            '<span class="spacer"></span>' +
            '<button @click="state.settingsOpen = !state.settingsOpen" style="font-size:11px;padding:2px 8px;">设置</button>' +
            '</div>',
        setup: function () { return { state: state }; }
    };

    // ======================== 组件：MessageList ========================
    const messageList = {
        template: '<div class="message-list" ref="list">' +
            '<div v-for="(m, i) in state.messages" :key="i" class="msg-line" :class="m.type">' +
            '<span class="time" v-if="m.time">{{ m.time }}</span>{{ m.text }}' +
            '</div>' +
            '</div>',
        setup: function () {
            var listRef = ref(null);
            onMounted(function () {
                if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight;
            });
            return { state: state };
        }
    };

    // ======================== 组件：InputBox ========================
    const inputBox = {
        template: '<div class="input-area">' +
            '<textarea v-model="input" @keydown.enter.exact="send" @keydown.enter.shift.exact="" placeholder="输入消息或指令，Enter 发送，Shift+Enter 换行"></textarea>' +
            '<button class="send-btn primary" @click="send">发送</button>' +
            '</div>',
        setup: function () {
            var input = ref('');
            function send() {
                var text = input.value.trim();
                if (!text) return;
                xechat.sendMessage(text);
                input.value = '';
            }
            return { input: input, send: send };
        }
    };

    // ======================== 组件：ToolMenu ========================
    const toolMenu = {
        template: '<div class="tool-grid">' +
            '<div v-for="t in state.tools" :key="t.index" class="tool-card" :class="{locked: t.requireLogin && !state.online}"' +
            ' @click="openTool(t)">' +
            '<div class="tool-icon">{{ toolIcon(t.index) }}</div>' +
            '<div class="tool-name">{{ t.name }}{{ t.requireLogin ? " (需登录)" : "" }}</div>' +
            '</div>' +
            '</div>',
        setup: function () {
            var icons = ['🌐','📖','🎮','📰','🎵','🎲','📝','🔍','📅','💬','🔗','📊','🎯','📁','⚙️','🧩'];
            function toolIcon(idx) { return icons[idx] || '🔧'; }
            function openTool(t) {
                if (t.requireLogin && !state.online) return;
                xechat.openTool(t.index);
            }
            return { state: state, toolIcon: toolIcon, openTool: openTool };
        }
    };

    // ======================== 组件：GameMenu ========================
    const gameMenu = {
        template: '<div class="game-grid">' +
            '<div v-for="g in state.games" :key="g.index" class="game-card" :class="{locked: g.requireLogin && !state.online}"' +
            ' @click="joinGame(g)">' +
            '<div class="game-icon">{{ gameIcon(g.index) }}</div>' +
            '<div class="game-name">{{ g.name }}{{ g.requireLogin ? " (需登录)" : "" }}</div>' +
            '</div>' +
            '</div>',
        setup: function () {
            var icons = ['♟️','📦','🧱','💣','🔢','⚪','🐍','🃏','2️⃣','🧠','🎯','🎪'];
            function gameIcon(idx) { return icons[idx] || '🎮'; }
            function joinGame(g) {
                if (g.requireLogin && !state.online) return;
                state.roomGameIndex = g.index;
                xechat.joinGame(g.index);
            }
            return { state: state, gameIcon: gameIcon, joinGame: joinGame };
        }
    };

    // ======================== 组件：SettingsPanel ========================
    const settingsPanel = {
        template: '<div class="settings-panel">' +
            '<h3 style="margin-bottom:16px;">设置</h3>' +
            '<div class="setting-item"><label>用户名</label><input v-model="username" placeholder="输入用户名"></div>' +
            '<div class="setting-item"><label>消息通知</label>' +
            '<select v-model="msgNotify"><option value="0">关闭</option><option value="1">开启</option></select></div>' +
            '<div class="setting-item"><label>JxBrowser License</label><input v-model="license" placeholder="JxBrowser License Key"></div>' +
            '<div style="margin-top:16px;display:flex;gap:8px;">' +
            '<button class="primary" @click="save">保存</button>' +
            '<button @click="state.settingsOpen = false">取消</button>' +
            '</div>' +
            '</div>',
        setup: function () {
            var username = ref(state.username);
            var msgNotify = ref('1');
            var license = ref('');
            onMounted(function () {
                try {
                    var cfg = JSON.parse(xechat.getConfig());
                    username.value = cfg.username || '';
                    msgNotify.value = String(cfg.msgNotify || 1);
                    license.value = cfg.jxBrowserLicense || '';
                } catch (e) {}
            });
            function save() {
                xechat.setConfig('username', username.value);
                xechat.setConfig('msgNotify', msgNotify.value);
                xechat.setConfig('jxBrowserLicense', license.value);
                state.settingsOpen = false;
            }
            return { state: state, username: username, msgNotify: msgNotify, license: license, save: save };
        }
    };

    // ======================== 组件：GameContainer（动态游戏渲染） ========================
    const gameContainer = {
        template: '<div class="game-container">' +
            '<div class="game-header">' +
            '<span>{{ currentGameName }}</span>' +
            '<button class="danger" @click="exitGame">退出游戏</button>' +
            '</div>' +
            '<div class="game-content" ref="content">' +
            '<canvas ref="canvas" class="game-canvas"></canvas>' +
            '</div>' +
            '</div>',
        setup: function () {
            var canvas = ref(null);
            var content = ref(null);
            var currentRenderer = null;
            var currentGameName = computed(function () {
                var idx = state.currentGameIndex;
                var g = state.games.find(function (x) { return x.index === idx; });
                return g ? g.name : '游戏';
            });

            function emitAction(action) {
                xechat.gameAction(state.currentGameIndex, action);
            }

            function loadRenderer() {
                teardownRenderer();
                var reg = window.xechatGames;
                if (!reg || !reg[state.currentGameIndex]) return;

                var renderer = reg[state.currentGameIndex];
                currentRenderer = renderer;
                if (renderer.setup) {
                    renderer.setup(canvas.value, emitAction);
                }
                if (renderer.render && state.gameStateData) {
                    nextTick(function () { renderer.render(canvas.value, state.gameStateData); });
                }
            }

            function teardownRenderer() {
                if (currentRenderer && currentRenderer.teardown) {
                    currentRenderer.teardown(canvas.value);
                }
                currentRenderer = null;
                // 清理 DOM 渲染残留
                if (content.value) {
                    var left = content.value.querySelectorAll('.game-2048-board, .game-2048-score');
                    left.forEach(function (el) { el.remove(); });
                }
            }

            function exitGame() { xechat.exitGame(); }

            onMounted(function () { loadRenderer(); });
            onUnmounted(function () { teardownRenderer(); });

            // 监听游戏状态变化
            watch(function () { return state.gameStateData; }, function (newData) {
                if (currentRenderer && currentRenderer.render && canvas.value) {
                    nextTick(function () { currentRenderer.render(canvas.value, newData); });
                }
            }, { deep: true });

            // 监听游戏切换
            watch(function () { return state.currentGameIndex; }, function () {
                nextTick(function () { loadRenderer(); });
            });

            return { state: state, canvas: canvas, content: content, currentGameName: currentGameName, exitGame: exitGame };
        }
    };

    // ======================== 组件：ToolContainer（动态工具渲染） ========================
    const toolContainer = {
        template: '<div class="tool-container">' +
            '<div class="tool-header">' +
            '<span>{{ currentToolName }}</span>' +
            '<button class="danger" @click="closeTool">关闭</button>' +
            '</div>' +
            '<div class="tool-body" ref="body"></div>' +
            '</div>',
        setup: function () {
            var body = ref(null);
            var currentTool = null;
            var currentToolName = computed(function () {
                var t = state.tools.find(function (x) { return x.index === state.currentTool; });
                return t ? t.name : '工具';
            });

            function loadTool() {
                teardownTool();
                if (!body.value) return;

                // 工具索引 → 工具 key 映射
                var toolKeyMap = { 0: 'browser', 1: 'reader' };
                var key = toolKeyMap[state.currentTool];
                if (!key) return;

                var tool = window.xechatTools && window.xechatTools[key];
                if (!tool) return;

                currentTool = tool;
                if (tool.setup) {
                    tool.setup(body.value, function (action) {
                        // 工具动作通过 CustomEvent 发送到 Java
                        window.dispatchEvent(new CustomEvent('xechat:toolAction', {
                            detail: { toolIndex: state.currentTool, action: action }
                        }));
                    });
                }
            }

            function teardownTool() {
                if (currentTool && currentTool.teardown && body.value) {
                    currentTool.teardown(body.value);
                }
                currentTool = null;
            }

            function closeTool() { xechat.toolClose(); }

            onMounted(function () { loadTool(); });
            onUnmounted(function () { teardownTool(); });

            // 工具切换时重新加载
            watch(function () { return state.currentTool; }, function () {
                nextTick(function () { loadTool(); });
            });

            return { state: state, body: body, currentToolName: currentToolName, closeTool: closeTool };
        }
    };

    // ======================== 组件：GameRoomPanel ========================
    const gameRoomPanel = {
        template: '<div class="room-panel">' +
            '<div class="room-header">' +
            '<span class="room-title">{{ state.roomGameName }} 房间</span>' +
            '<button class="danger" @click="leaveRoom">离开房间</button>' +
            '</div>' +
            '<div class="room-info">' +
            '<span>房间号: {{ state.roomId }}</span>' +
            '<span>人数: {{ state.roomPlayers.length }} / {{ state.roomNums }}</span>' +
            '<span v-if="state.isHomeowner" class="tag-homeowner">房主</span>' +
            '</div>' +
            '<div class="room-players">' +
            '<div v-for="(p, i) in state.roomPlayers" :key="i" class="room-player-item">' +
            '<span class="player-index">{{ i + 1 }}.</span>' +
            '<span class="player-name">{{ p.username }}</span>' +
            '<span v-if="state.isHomeowner && state.roomId && p.id === state.roomId" class="tag-homeowner">房主</span>' +
            '<span class="player-status" :class="{ready: p.readied}">{{ p.readied ? "已准备" : "未准备" }}</span>' +
            '</div>' +
            '<div v-if="state.roomPlayers.length === 0" class="room-empty">等待玩家加入...</div>' +
            '</div>' +
            '<div class="room-actions">' +
            '<button class="primary" @click="toggleReady" v-if="!state.isHomeowner">{{ state.roomReady ? "取消准备" : "准备" }}</button>' +
            '<div class="invite-row" v-if="state.isHomeowner">' +
            '<input v-model="inviteName" placeholder="输入玩家昵称邀请" @keydown.enter="doInvite">' +
            '<button @click="doInvite">邀请</button>' +
            '</div>' +
            '</div>' +
            '</div>',
        setup: function () {
            var inviteName = ref('');
            function toggleReady() {
                if (state.roomReady) {
                    xechat.roomUnready();
                    state.roomReady = false;
                } else {
                    xechat.roomReady();
                    state.roomReady = true;
                }
            }
            function doInvite() {
                var name = inviteName.value.trim();
                if (!name) return;
                xechat.roomInvite(name);
                inviteName.value = '';
            }
            function leaveRoom() {
                xechat.roomLeave();
                resetRoomState();
                navigate('chat');
            }
            return { state: state, inviteName: inviteName, toggleReady: toggleReady, doInvite: doInvite, leaveRoom: leaveRoom };
        }
    };

    // ======================== 组件：RightPanel ========================
    const rightPanel = {
        template: '<div class="right-panel">' +
            '<tool-menu v-if="state.currentRoute === \'chat\' && !state.toolOpen" />' +
            '<tool-container v-if="state.toolOpen && state.currentRoute === \'chat\'" />' +
            '<game-menu v-if="state.currentRoute === \'game\' && !state.gamePlaying && !state.inRoom" />' +
            '<game-container v-if="state.gamePlaying && state.currentRoute === \'game\' && !state.inRoom" />' +
            '<game-room-panel v-if="state.inRoom && state.currentRoute === \'room\'" />' +
            '<settings-panel v-if="state.settingsOpen" />' +
            '</div>',
        components: { toolMenu: toolMenu, gameMenu: gameMenu, gameContainer: gameContainer, toolContainer: toolContainer, settingsPanel: settingsPanel, gameRoomPanel: gameRoomPanel }
    };

    // ======================== 主应用 ========================
    const App = {
        template: '<status-bar />' +
            '<div class="main-layout">' +
            '<div class="message-panel">' +
            '<message-list />' +
            '<input-box />' +
            '</div>' +
            '<right-panel />' +
            '</div>',
        components: { statusBar: statusBar, messageList: messageList, inputBox: inputBox, rightPanel: rightPanel }
    };

    // ======================== 启动 ========================
    var app = createApp(App);
    app.config.unwrapInjectedRef = true;
    app.mount('#app');

    // 初始加载
    refreshState();
    loadTools();
    loadGames();

    // 定时刷新状态
    setInterval(refreshState, 5000);
})();
