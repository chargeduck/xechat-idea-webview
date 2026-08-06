/**
 * UNO 渲染器。
 * 卡牌游戏：手牌扇形排列、出牌堆、颜色选择器。
 */
(function () {
    'use strict';

    var CARD_W = 80, CARD_H = 120;
    var W = 800, H = 520;
    var PILE_X = W / 2, PILE_Y = H * 0.38;

    var COLORS = {
        red:    '#e74c3c',
        green:  '#27ae60',
        blue:   '#2980b9',
        yellow: '#f1c40f',
        wild:   '#2c3e50'
    };

    var COLOR_LIGHT = {
        red:    '#fadbd8',
        green:  '#d5f5e3',
        blue:   '#d6eaf8',
        yellow: '#fef9e7',
        wild:   '#eaecee'
    };

    // 手牌扇形参数
    var MAX_ANGLE = Math.PI * 0.7;
    var RADIUS = 260;

    function drawCard(ctx, x, y, card, scale) {
        scale = scale || 1;
        var w = CARD_W * scale, h = CARD_H * scale;
        var r = 6 * scale;

        // 阴影
        ctx.fillStyle = 'rgba(0,0,0,0.2)';
        roundRect(ctx, x + 2, y + 2, w, h, r, true, false);

        // 牌面
        var bg = card.color ? COLORS[card.color] : COLORS.wild;
        ctx.fillStyle = card.color ? '#fff' : '#f8f8f8';
        roundRect(ctx, x, y, w, h, r, true, false);
        ctx.strokeStyle = '#333';
        ctx.lineWidth = 1;
        roundRect(ctx, x, y, w, h, r, false, true);

        // 彩色圆圈（非万能牌）
        if (card.color && card.color !== 'wild') {
            ctx.fillStyle = bg;
            ctx.beginPath();
            ctx.ellipse(x + w / 2, y + h / 2, w * 0.38, h * 0.38, 0, 0, Math.PI * 2);
            ctx.fill();
        } else if (card.color === 'wild') {
            // 四色扇形
            var cx = x + w / 2, cy = y + h / 2, rs = w * 0.35;
            ['red', 'blue', 'yellow', 'green'].forEach(function (clr, i) {
                ctx.fillStyle = COLORS[clr];
                ctx.beginPath();
                ctx.moveTo(cx, cy);
                ctx.arc(cx, cy, rs, i * Math.PI / 2, (i + 1) * Math.PI / 2);
                ctx.fill();
            });
        }

        // 文字
        ctx.fillStyle = card.color && card.color !== 'wild' ? '#fff' : '#222';
        ctx.font = 'bold ' + (20 * scale) + 'px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';

        var label;
        if (card.value === 'skip') label = '⊘';
        else if (card.value === 'reverse') label = '⟲';
        else if (card.value === 'draw2') label = '+2';
        else if (card.value === 'wild') label = 'W';
        else if (card.value === 'wild4') label = '+4';
        else label = String(card.value);

        ctx.fillText(label, x + w / 2, y + h / 2);

        // 角标
        ctx.font = 'bold ' + (11 * scale) + 'px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(label, x + 5 * scale, y + 14 * scale);
        ctx.textAlign = 'right';
        ctx.fillText(label, x + w - 5 * scale, y + h - 4 * scale);
    }

    function roundRect(ctx, x, y, w, h, r, fill, stroke) {
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.lineTo(x + w - r, y);
        ctx.arcTo(x + w, y, x + w, y + r, r);
        ctx.lineTo(x + w, y + h - r);
        ctx.arcTo(x + w, y + h, x + w - r, y + h, r);
        ctx.lineTo(x + r, y + h);
        ctx.arcTo(x, y + h, x, y + h - r, r);
        ctx.lineTo(x, y + r);
        ctx.arcTo(x, y, x + r, y, r);
        ctx.closePath();
        if (fill) ctx.fill();
        if (stroke) ctx.stroke();
    }

    function drawHand(ctx, hand, state) {
        var n = hand.length;
        if (n === 0) return;
        if (n > 30) n = 30; // 大量手牌截断

        var totalAngle;
        if (n <= 1) {
            totalAngle = 0;
        } else if (n <= 7) {
            totalAngle = MAX_ANGLE * (n - 1) / 6;
        } else {
            totalAngle = MAX_ANGLE;
        }

        for (var i = 0; i < n; i++) {
            var t = n === 1 ? 0.5 : i / (n - 1);
            var angle = Math.PI / 2 + totalAngle * (t - 0.5);
            var cx = W / 2 + Math.cos(angle) * RADIUS;
            var cy = H * 0.78 + Math.sin(angle) * RADIUS * 0.5;
            var card = hand[i];
            var scale = 0.8;
            var hover = state.selectedCard === i;

            if (hover) {
                cy -= 20;
                scale = 0.95;
            }

            ctx.save();
            ctx.translate(cx, cy);
            var rotAngle = angle - Math.PI / 2;
            ctx.rotate(rotAngle * 0.25);
            drawCard(ctx, -CARD_W * scale / 2, -CARD_H * scale / 2, card, scale);
            ctx.restore();
        }
    }

    function drawPlayerInfo(ctx, state) {
        var players = state.players || [];
        var myIdx = state.myIndex || 0;

        ctx.font = 'bold 13px sans-serif';
        ctx.textAlign = 'center';

        players.forEach(function (p, i) {
            var x, y;
            if (i === myIdx) {
                x = W / 2;
                y = H - 16;
            } else if (players.length <= 4) {
                var positions = [
                    { x: W - 60, y: 30 },
                    { x: 60, y: 30 },
                    { x: W - 60, y: H - 40 },
                ];
                var offset = (i < myIdx) ? i : i - 1;
                if (offset < positions.length) {
                    x = positions[offset].x;
                    y = positions[offset].y;
                }
            } else {
                var ang = (i / players.length) * Math.PI * 2;
                x = W / 2 + Math.cos(ang) * 180;
                y = 30 + Math.sin(ang) * 60;
            }

            if (x === undefined || y === undefined) return;

            ctx.fillStyle = i === state.currentPlayer ? '#f39c12' : '#aaa';
            ctx.fillText(p.name + ' (' + p.cardCount + '张)', x, y);

            if (i === state.currentPlayer) {
                ctx.fillStyle = state.direction === 1 ? '#27ae60' : '#e74c3c';
                ctx.fillText(state.direction === 1 ? '▶' : '◀', x, y + 16);
            }
        });
    }

    function drawColorPicker(ctx) {
        var colorNames = ['red', 'blue', 'green', 'yellow'];
        var startX = W / 2 - 140, y = PILE_Y - 40;
        colorNames.forEach(function (c, i) {
            var x = startX + i * 80;
            ctx.fillStyle = COLORS[c];
            ctx.beginPath();
            ctx.arc(x, y, 24, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = '#333';
            ctx.lineWidth = 2;
            ctx.stroke();

            ctx.fillStyle = '#fff';
            ctx.font = 'bold 12px sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(c === 'red' ? 'R' : c === 'blue' ? 'B' : c === 'green' ? 'G' : 'Y', x, y);
        });
    }

    function drawUnoButton(ctx) {
        // UNO 按钮（手牌只剩 1 张时可点）
        var x = W - 100, y = H * 0.6;
        ctx.fillStyle = '#e74c3c';
        ctx.beginPath();
        ctx.arc(x, y, 30, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 18px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('UNO', x, y);
    }

    // 点击检测
    function handleClick(e, canvas, state, emit) {
        var rect = canvas.getBoundingClientRect();
        var mx = e.clientX - rect.left;
        var my = e.clientY - rect.top;

        // 颜色选择器
        if (state.needChooseColor) {
            var colorNames = ['red', 'blue', 'green', 'yellow'];
            var startX = W / 2 - 140;
            for (var i = 0; i < 4; i++) {
                var cx = startX + i * 80;
                var dist = Math.sqrt((mx - cx) * (mx - cx) + (my - (PILE_Y - 40)) * (my - (PILE_Y - 40)));
                if (dist < 30) {
                    emit(JSON.stringify({ action: 'chooseColor', color: colorNames[i] }));
                    return;
                }
            }
            return;
        }

        // UNO 按钮
        var unoX = W - 100, unoY = H * 0.6;
        if (Math.sqrt((mx - unoX) * (mx - unoX) + (my - unoY) * (my - unoY)) < 30) {
            emit(JSON.stringify({ action: 'uno' }));
            return;
        }

        // 手牌点击
        var hand = state.hand || [];
        var n = Math.min(hand.length, 30);
        if (n === 0) return;

        var totalAngle = n <= 1 ? 0 : n <= 7 ? MAX_ANGLE * (n - 1) / 6 : MAX_ANGLE;

        for (var j = 0; j < n; j++) {
            var t = n === 1 ? 0.5 : j / (n - 1);
            var angle = Math.PI / 2 + totalAngle * (t - 0.5);
            var cardX = W / 2 + Math.cos(angle) * RADIUS;
            var cardY = H * 0.78 + Math.sin(angle) * RADIUS * 0.5;

            var cw = CARD_W * 0.8, ch = CARD_H * 0.8;
            if (Math.abs(mx - cardX) < cw / 2 && Math.abs(my - cardY) < ch / 2) {
                emit(JSON.stringify({ action: 'play', cardIndex: j }));
                return;
            }
        }

        // 摸牌区
        if (Math.abs(mx - (PILE_X - 70)) < CARD_W / 2 && Math.abs(my - PILE_Y) < CARD_H / 2) {
            emit(JSON.stringify({ action: 'draw' }));
            return;
        }
    }

    function handleKey(e, emit) {
        if (e.key === 'Enter' || e.key === ' ') {
            emit(JSON.stringify({ action: 'draw' }));
        } else if (e.key === 'u' || e.key === 'U') {
            emit(JSON.stringify({ action: 'uno' }));
        }
    }

    window.xechatGames = window.xechatGames || {};
    window.xechatGames[4] = {
        name: 'UNO',
        canvasW: W,
        canvasH: H,
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
            canvas.height = H;
            this.canvasW = W;
            this.canvasH = H;
            this._state = state;

            var ctx = canvas.getContext('2d');

            // 背景
            ctx.fillStyle = '#1a5c2a';
            ctx.fillRect(0, 0, W, H);

            if (!state) return;

            // 出牌堆
            if (state.currentCard) {
                drawCard(ctx, PILE_X - CARD_W / 2, PILE_Y - CARD_H / 2, state.currentCard, 1);
            }

            // 摸牌堆（暗牌）
            ctx.fillStyle = '#2c3e50';
            roundRect(ctx, PILE_X - 70 - CARD_W / 2, PILE_Y - CARD_H / 2, CARD_W, CARD_H, 8, true, true);
            ctx.fillStyle = '#fff';
            ctx.font = 'bold 16px sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText('摸牌', PILE_X - 70, PILE_Y);

            // 颜色指示器
            if (state.currentColor) {
                ctx.fillStyle = COLORS[state.currentColor] || '#333';
                ctx.beginPath();
                ctx.arc(PILE_X, PILE_Y - CARD_H / 2 - 20, 12, 0, Math.PI * 2);
                ctx.fill();
                ctx.strokeStyle = '#fff';
                ctx.lineWidth = 2;
                ctx.stroke();
            }

            // 手牌
            drawHand(ctx, state.hand || [], state);
            drawPlayerInfo(ctx, state);

            // 颜色选择器
            if (state.needChooseColor) {
                ctx.fillStyle = 'rgba(0,0,0,0.5)';
                ctx.fillRect(0, 0, W, H);
                drawColorPicker(ctx);
                ctx.fillStyle = '#fff';
                ctx.font = 'bold 18px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText('选择颜色', W / 2, PILE_Y - 80);
            }

            // UNO 按钮
            if (state.hand && state.hand.length <= 2 && state.currentPlayer === state.myIndex) {
                drawUnoButton(ctx);
            }

            // 消息
            if (state.message) {
                ctx.fillStyle = 'rgba(0,0,0,0.6)';
                ctx.fillRect(W / 2 - 150, 8, 300, 28);
                ctx.fillStyle = '#fff';
                ctx.font = '14px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(state.message, W / 2, 26);
            }

            // 游戏结束
            if (state.gameOver) {
                ctx.fillStyle = 'rgba(0,0,0,0.6)';
                ctx.fillRect(0, H / 2 - 40, W, 80);
                ctx.fillStyle = '#f1c40f';
                ctx.font = 'bold 32px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(state.winner ? state.winner + ' 获胜!' : '游戏结束', W / 2, H / 2 + 10);
            }
        },
        teardown: function (canvas) {
            if (this._clickHandler) canvas.removeEventListener('click', this._clickHandler);
            if (this._keyHandler) document.removeEventListener('keydown', this._keyHandler);
        }
    };
})();
