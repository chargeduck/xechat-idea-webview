/**
 * 贪吃蛇渲染器。
 */
(function () {
    'use strict';

    var COLS = 30, ROWS = 20;
    var CELL = 20;
    var W = COLS * CELL;
    var H = ROWS * CELL;

    function draw(ctx, state) {
        // 背景
        ctx.fillStyle = '#1a1a2e';
        ctx.fillRect(0, 0, W, H);

        // 网格
        ctx.strokeStyle = 'rgba(255,255,255,0.03)';
        ctx.lineWidth = 0.5;
        for (var r = 0; r < ROWS; r++) {
            for (var c = 0; c < COLS; c++) {
                ctx.strokeRect(c * CELL, r * CELL, CELL, CELL);
            }
        }

        if (!state) return;

        // 食物
        if (state.food) {
            ctx.fillStyle = '#ff4757';
            ctx.beginPath();
            ctx.arc(state.food.x * CELL + CELL / 2, state.food.y * CELL + CELL / 2, CELL / 2 - 1, 0, Math.PI * 2);
            ctx.fill();
        }

        // 蛇身
        var snake = state.snake || [];
        snake.forEach(function (seg, i) {
            var alpha = 1 - i / (snake.length + 10);
            ctx.fillStyle = 'rgba(46, 213, 115, ' + Math.max(alpha, 0.4) + ')';
            ctx.fillRect(seg.x * CELL + 1, seg.y * CELL + 1, CELL - 2, CELL - 2);
        });

        // 蛇头
        if (snake.length > 0) {
            var head = snake[0];
            ctx.fillStyle = '#2ed573';
            ctx.fillRect(head.x * CELL + 1, head.y * CELL + 1, CELL - 2, CELL - 2);
            // 眼睛
            ctx.fillStyle = '#fff';
            ctx.fillRect(head.x * CELL + CELL * 0.3, head.y * CELL + CELL * 0.25, CELL * 0.18, CELL * 0.2);
            ctx.fillRect(head.x * CELL + CELL * 0.55, head.y * CELL + CELL * 0.25, CELL * 0.18, CELL * 0.2);
            ctx.fillStyle = '#000';
            ctx.fillRect(head.x * CELL + CELL * 0.35, head.y * CELL + CELL * 0.3, CELL * 0.1, CELL * 0.12);
            ctx.fillRect(head.x * CELL + CELL * 0.6, head.y * CELL + CELL * 0.3, CELL * 0.1, CELL * 0.12);
        }

        // 分数
        ctx.fillStyle = '#aaa';
        ctx.font = '13px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText('Score: ' + (state.score || 0), 8, H - 6);

        // 游戏结束
        if (state.gameOver) {
            ctx.fillStyle = 'rgba(0,0,0,0.5)';
            ctx.fillRect(0, H / 2 - 25, W, 50);
            ctx.fillStyle = '#ff4757';
            ctx.font = 'bold 22px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText('GAME OVER', W / 2, H / 2 + 8);
            ctx.textAlign = 'start';
        }
    }

    function handleKey(e, emit) {
        var map = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right',
                    w: 'up', s: 'down', a: 'left', d: 'right' };
        var dir = map[e.key];
        if (dir) {
            e.preventDefault();
            emit(JSON.stringify({ action: 'turn', direction: dir }));
        }
    }

    window.xechatGames = window.xechatGames || {};
    window.xechatGames[6] = {
        name: '贪吃蛇',
        canvasW: W,
        canvasH: H,
        setup: function (canvas, emit) {
            var self = this;
            self._emit = emit;
            self._keyHandler = function (e) { handleKey(e, emit); };
            document.addEventListener('keydown', self._keyHandler);
        },
        render: function (canvas, state) {
            canvas.width = W;
            canvas.height = H;
            this.canvasW = W;
            this.canvasH = H;
            draw(canvas.getContext('2d'), state);
        },
        teardown: function () {
            if (this._keyHandler) {
                document.removeEventListener('keydown', this._keyHandler);
            }
        }
    };
})();
