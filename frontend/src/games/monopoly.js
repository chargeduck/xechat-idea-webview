/**
 * 大富翁（Monopoly）渲染器。
 * 40 格环形棋盘 + 地产归属 + 多玩家标记。
 */
(function () {
    'use strict';

    var CELL = 54;         // 普通格子尺寸
    var CORNER = 70;       // 角落格子尺寸
    var W = 11 * CELL;
    var H = 11 * CELL;
    var PADDING = 10;

    var CELL_NAMES = [
        '起点','地中海','宝箱','波罗地','所得税',
        '铁路1','东方','机会','佛蒙特','州税',
        '铁路2','圣查','电力','州立','圣詹',
        '宾夕','铁路3','短线','公园','肯塔',
        '机会','印第','伊利','B&O','大西',
        '水厂','玛文','社区','苏伊','地中海',
        '铁路4','纽约','宝箱','田纳','社区',
        '公园地','机会','佛吉','宾州','铁路',
        '机会','北卡','社区','宝箱','铁路',
        '铁路','机会','公园','宝箱','去监狱'
    ];

    var GROUP_COLORS = [
        null,          // 0 起点
        '#8B4513',     // 1-2 棕色
        null,          // 2 宝箱
        '#8B4513',     // 3
        null,          // 4 所得税
        '#333',        // 5 铁路
        '#87CEEB',     // 6-8 浅蓝
        null,          // 7 机会
        '#87CEEB',     // 8
        null,          // 9
        '#333',        // 10 铁路
        '#DA70D6',     // 11-13 紫色
        null,          // 12 电力
        '#DA70D6',     // 13
        '#FF8C00',     // 14-15 橙色
        '#FF8C00',
        '#333',        // 16 铁路
        '#DC143C',     // 17-18 红色
        null,          // 18
        '#DC143C',     // 19
        null,          // 20
        '#FFD700',     // 21-23 黄色
        '#FFD700',
        '#333',        // 23
        '#FFD700',     // 24
        null,          // 25
        '#008000',     // 26-27 绿色
        null,          // 27
        '#008000',     // 28
        null,          // 29
        '#0000CD',     // 30-31 深蓝
        '#333',        // 31
        '#0000CD',     // 32
        null,          // 33
        null,          // 34
        '#333',        // 35
        null,          // 36
        '#2E8B57',     // 37-38
        null,          // 38
        null,          // 39
    ];

    // 位置 → (row, col) 映射
    function posToRC(pos) {
        if (pos <= 10)      return { row: 10, col: pos };
        if (pos <= 19)      return { row: 19 - pos, col: 0 };
        if (pos <= 30)      return { row: 0, col: 30 - pos };
        return { row: pos - 30, col: 10 };
    }

    function cellBounds(r, c) {
        var isCorner = (r === 0 || r === 10) && (c === 0 || c === 10);
        if (isCorner) {
            return {
                x: c * CELL - (c === 10 ? CORNER - CELL : 0),
                y: r * CELL - (r === 10 ? CORNER - CELL : 0),
                w: CORNER, h: CORNER
            };
        }
        // 上下排
        if (r === 0 || r === 10) {
            var ox = (c === 0) ? CORNER - CELL : 0;
            return { x: c * CELL + ox, y: r * CELL, w: CELL, h: CORNER };
        }
        // 左右排
        if (c === 0 || c === 10) {
            var oy = (r === 10) ? CORNER - CELL : 0;
            return { x: c * CELL, y: r * CELL + oy, w: CORNER, h: CELL };
        }
        return null;
    }

    function drawCell(ctx, r, c, state) {
        var cell = state.cells || [];
        var pos = -1;

        // 反查 pos
        if (r === 10) pos = c;
        else if (c === 0) pos = 19 - r;
        else if (r === 0) pos = 30 - c;
        else if (c === 10) pos = 30 + r;
        if (pos < 0 || pos >= 40) return;

        var bounds = cellBounds(r, c);
        if (!bounds) return;
        var x = bounds.x, y = bounds.y, w = bounds.w, h = bounds.h;

        var cellData = (cell && cell[pos]) ? cell[pos] : {};
        var groupColor = GROUP_COLORS[pos];

        // 背景
        ctx.fillStyle = '#e8f5e9';
        ctx.fillRect(x, y, w, h);
        ctx.strokeStyle = '#333';
        ctx.lineWidth = 1;
        ctx.strokeRect(x, y, w, h);

        // 颜色条
        if (groupColor && pos % 5 !== 0 && pos !== 2 && pos !== 7 && pos !== 17 && pos !== 22 && pos !== 33 && pos !== 36) {
            var barH = (r === 0 || r === 10) ? 7 : 5;
            ctx.fillStyle = groupColor;
            ctx.fillRect(x, y, w, barH);
            if (cellData.owner != null) {
                ctx.fillStyle = cellData.ownerColor || '#555';
                ctx.fillRect(x, y + barH, w, 3);
            }
        }

        // 名称
        ctx.fillStyle = '#222';
        var fontSize = (r === 0 || r === 10) ? 9 : 7;
        ctx.font = fontSize + 'px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        var label = cellData.name || CELL_NAMES[pos] || ('#' + pos);
        ctx.fillText(label, x + w / 2, y + h * 0.35);

        // 房价
        if (cellData.price && cellData.owner == null) {
            ctx.fillStyle = '#666';
            ctx.font = '7px sans-serif';
            ctx.fillText('$' + cellData.price, x + w / 2, y + h * 0.55);
        }

        // 房屋
        if (cellData.houses > 0) {
            ctx.fillStyle = '#2e7d32';
            var hx = x + 3;
            for (var i = 0; i < Math.min(cellData.houses, 4); i++) {
                ctx.fillRect(hx + i * 6, y + h - 9, 5, 5);
            }
            if (cellData.houses === 5) {
                ctx.fillStyle = '#c62828';
                ctx.fillRect(hx, y + h - 9, 8, 7);
            }
        }

        // 抵押标记
        if (cellData.mortgaged) {
            ctx.fillStyle = 'rgba(255,0,0,0.25)';
            ctx.fillRect(x, y, w, h);
        }
    }

    function drawPlayers(ctx, state) {
        var players = state.players || [];
        var occupied = {}; // pos → count

        players.forEach(function (p, i) {
            var pos = p.position;
            var rc = posToRC(pos);
            var bounds = cellBounds(rc.row, rc.col);
            if (!bounds) return;

            var count = occupied[pos] || 0;
            occupied[pos] = count + 1;

            var cx = bounds.x + bounds.w * 0.3 + (count % 3) * 12;
            var cy = bounds.y + bounds.h * 0.78 - Math.floor(count / 3) * 12;

            ctx.fillStyle = p.color || ['#e74c3c','#3498db','#f1c40f','#2ecc71','#9b59b6','#e67e22'][i % 6];
            ctx.beginPath();
            ctx.arc(cx, cy, 7, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = '#fff';
            ctx.lineWidth = 1.5;
            ctx.stroke();

            // 当前玩家标记
            if (i === state.currentPlayer) {
                ctx.strokeStyle = '#f39c12';
                ctx.lineWidth = 2;
                ctx.beginPath();
                ctx.arc(cx, cy, 9, 0, Math.PI * 2);
                ctx.stroke();
            }
        });
    }

    function drawCenter(ctx, state) {
        var x = CELL, y = CELL;
        var cw = CELL * 9, ch = CELL * 9;

        // 中心区域
        ctx.fillStyle = '#c8e6c9';
        ctx.fillRect(x, y, cw, ch);
        ctx.strokeStyle = '#333';
        ctx.lineWidth = 2;
        ctx.strokeRect(x, y, cw, ch);

        // 标题
        ctx.fillStyle = '#1b5e20';
        ctx.font = 'bold 28px serif';
        ctx.textAlign = 'center';
        ctx.fillText('大富翁', x + cw / 2, y + 40);

        // 骰子
        if (state.dice && state.dice.length >= 2) {
            drawDice(ctx, x + cw / 2 - 30, y + 80, state.dice[0]);
            drawDice(ctx, x + cw / 2 + 10, y + 80, state.dice[1]);
        }

        // 玩家信息
        ctx.font = '13px sans-serif';
        ctx.textAlign = 'left';
        var players = state.players || [];
        players.forEach(function (p, i) {
            var py = y + 140 + i * 22;
            ctx.fillStyle = p.color || '#333';
            ctx.fillText('●', x + 20, py);
            ctx.fillStyle = '#222';
            ctx.fillText(p.name + '  $' + (p.money || 0), x + 36, py);
        });

        // 消息
        if (state.message) {
            ctx.fillStyle = '#fff';
            ctx.fillRect(x + 10, y + ch - 36, cw - 20, 28);
            ctx.strokeStyle = '#333';
            ctx.strokeRect(x + 10, y + ch - 36, cw - 20, 28);
            ctx.fillStyle = '#222';
            ctx.font = 'bold 14px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText(state.message, x + cw / 2, y + ch - 22);
        }
    }

    function drawDice(ctx, x, y, value) {
        var s = 34;
        ctx.fillStyle = '#fff';
        ctx.fillRect(x, y, s, s);
        ctx.strokeStyle = '#333';
        ctx.lineWidth = 2;
        ctx.strokeRect(x, y, s, s);

        ctx.fillStyle = '#333';
        var positions = {
            1: [[0.5,0.5]],
            2: [[0.25,0.25],[0.75,0.75]],
            3: [[0.25,0.25],[0.5,0.5],[0.75,0.75]],
            4: [[0.25,0.25],[0.75,0.25],[0.25,0.75],[0.75,0.75]],
            5: [[0.25,0.25],[0.75,0.25],[0.5,0.5],[0.25,0.75],[0.75,0.75]],
            6: [[0.25,0.2],[0.75,0.2],[0.25,0.5],[0.75,0.5],[0.25,0.8],[0.75,0.8]]
        };
        var dots = positions[value] || [];
        dots.forEach(function (d) {
            ctx.beginPath();
            ctx.arc(x + d[0] * s, y + d[1] * s, 3, 0, Math.PI * 2);
            ctx.fill();
        });
    }

    function handleClick(e, canvas, state, emit) {
        var rect = canvas.getBoundingClientRect();
        var mx = e.clientX - rect.left;
        var my = e.clientY - rect.top;

        // 检测点击的格子
        for (var r = 0; r <= 10; r++) {
            for (var c = 0; c <= 10; c++) {
                if (r > 0 && r < 10 && c > 0 && c < 10) continue; // 中间区域不算棋盘格
                var bounds = cellBounds(r, c);
                if (!bounds) continue;
                if (mx >= bounds.x && mx <= bounds.x + bounds.w &&
                    my >= bounds.y && my <= bounds.y + bounds.h) {
                    // 反推 pos
                    var pos;
                    if (r === 10) pos = c;
                    else if (c === 0) pos = 19 - r;
                    else if (r === 0) pos = 30 - c;
                    else if (c === 10) pos = 30 + r;
                    if (pos >= 0 && pos < 40) {
                        emit(JSON.stringify({ action: 'clickCell', position: pos }));
                    }
                    return;
                }
            }
        }
    }

    function handleKey(e, emit) {
        if (e.key === 'Enter' || e.key === ' ') {
            emit(JSON.stringify({ action: 'roll' }));
        } else if (e.key === 'b' || e.key === 'B') {
            emit(JSON.stringify({ action: 'buy' }));
        } else if (e.key === 'a' || e.key === 'A') {
            emit(JSON.stringify({ action: 'auction' }));
        } else if (e.key === 't' || e.key === 'T') {
            emit(JSON.stringify({ action: 'trade' }));
        } else if (e.key === 'h' || e.key === 'H') {
            emit(JSON.stringify({ action: 'buildHouse' }));
        }
    }

    window.xechatGames = window.xechatGames || {};
    window.xechatGames[9] = {
        name: '大富翁',
        canvasW: W,
        canvasH: H + 30,
        setup: function (canvas, emit) {
            var self = this;
            self._emit = emit;
            self._state = null;
            self._clickHandler = function (e) { handleClick(e, canvas, self._state, emit); };
            self._keyHandler = function (e) { handleKey(e, emit); };
            canvas.addEventListener('click', self._clickHandler);
            document.addEventListener('keydown', self._keyHandler);
        },
        render: function (canvas, state) {
            canvas.width = W;
            canvas.height = H + 30;
            this.canvasW = W;
            this.canvasH = H + 30;
            this._state = state;

            var ctx = canvas.getContext('2d');
            ctx.fillStyle = '#a5d6a7';
            ctx.fillRect(0, 0, W, H + 30);

            // 绘制 40 格
            for (var r = 0; r <= 10; r++) {
                for (var c = 0; c <= 10; c++) {
                    if (r > 0 && r < 10 && c > 0 && c < 10) continue;
                    drawCell(ctx, r, c, state);
                }
            }

            if (!state) return;

            drawCenter(ctx, state);
            drawPlayers(ctx, state);

            // 操作提示
            var hints = [];
            if (state.phase === 'roll') hints.push('按 Enter/空格 掷骰子');
            if (state.phase === 'buy') hints.push('按 B 购买地产  |  A 拍卖');
            if (state.phase === 'build') hints.push('按 H 建造房屋  |  点击格子选择');
            if (state.phase === 'trade') hints.push('按 T 发起交易');
            if (state.phase === 'auction') hints.push('拍卖中...');

            ctx.fillStyle = '#555';
            ctx.font = '11px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText(hints.join('    '), W / 2, H + 20);

            // 游戏结束
            if (state.gameOver) {
                ctx.fillStyle = 'rgba(0,0,0,0.5)';
                ctx.fillRect(0, H / 2 - 30, W, 60);
                ctx.fillStyle = '#f1c40f';
                ctx.font = 'bold 28px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(state.winner + ' 获胜!', W / 2, H / 2 + 10);
            }
        },
        teardown: function (canvas) {
            if (this._clickHandler) canvas.removeEventListener('click', this._clickHandler);
            if (this._keyHandler) document.removeEventListener('keydown', this._keyHandler);
        }
    };
})();
