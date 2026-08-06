/**
 * 推箱子渲染器。
 * 网格迷宫游戏，玩家推动箱子到目标点。
 */
(function () {
    'use strict';

    var CELL_SIZE = 48;
    var COLS = 0, ROWS = 0;

    var TILES = {
        0: '#d4c4a8', // 空地
        1: '#555',    // 墙
        2: '#e8c840', // 目标点
        3: '#a0522d', // 箱子
        4: '#e8c840', // 箱子在目标上（黄色目标+棕色箱子）
        5: '#3498db', // 玩家
        6: '#3498db', // 玩家在目标上
    };

    var TILE_SYMBOLS = {
        0: '', 1: '', 2: '◎', 3: '■', 4: '★', 5: '●', 6: '●'
    };

    function drawMap(ctx, map, W, H) {
        ROWS = map.length;
        COLS = map[0].length;

        for (var r = 0; r < ROWS; r++) {
            for (var c = 0; c < COLS; c++) {
                var tile = map[r][c];
                ctx.fillStyle = TILES[tile] || '#d4c4a8';
                ctx.fillRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                // 网格线
                ctx.strokeStyle = 'rgba(0,0,0,0.1)';
                ctx.strokeRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                // 特殊符号
                var sym = TILE_SYMBOLS[tile];
                if (sym) {
                    ctx.fillStyle = tile === 3 ? '#fff' : tile >= 5 ? '#fff' : '#333';
                    ctx.font = '20px sans-serif';
                    ctx.textAlign = 'center';
                    ctx.textBaseline = 'middle';
                    ctx.fillText(sym, c * CELL_SIZE + CELL_SIZE / 2, r * CELL_SIZE + CELL_SIZE / 2);
                }

                // 目标点十字线（空目标）
                if (tile === 2) {
                    ctx.strokeStyle = '#d4a000';
                    ctx.lineWidth = 2;
                    var cx = c * CELL_SIZE + CELL_SIZE / 2;
                    var cy = r * CELL_SIZE + CELL_SIZE / 2;
                    ctx.beginPath();
                    ctx.moveTo(cx - 6, cy); ctx.lineTo(cx + 6, cy);
                    ctx.moveTo(cx, cy - 6); ctx.lineTo(cx, cy + 6);
                    ctx.stroke();
                    ctx.lineWidth = 1;
                }
            }
        }
    }

    function handleKey(e, emit) {
        var dirs = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right',
                     w: 'up', s: 'down', a: 'left', d: 'right',
                     W: 'up', S: 'down', A: 'left', D: 'right' };
        var dir = dirs[e.key];
        if (dir) {
            e.preventDefault();
            emit(JSON.stringify({ action: 'move', direction: dir }));
        }
    }

    window.xechatGames = window.xechatGames || {};
    window.xechatGames[1] = {
        name: '推箱子',
        canvasW: 0,
        canvasH: 0,
        setup: function (canvas, emit) {
            var self = this;
            self._emit = emit;
            self._keyHandler = function (e) { handleKey(e, emit); };
            document.addEventListener('keydown', self._keyHandler);
        },
        render: function (canvas, state) {
            if (!state || !state.map) return;
            var map = state.map;
            ROWS = map.length;
            COLS = map[0].length;
            var W = COLS * CELL_SIZE;
            var H = ROWS * CELL_SIZE;
            canvas.width = W;
            canvas.height = H;
            this.canvasW = W;
            this.canvasH = H;

            var ctx = canvas.getContext('2d');
            drawMap(ctx, map, W, H);

            if (state.message) {
                ctx.fillStyle = '#333';
                ctx.font = '14px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(state.message, W / 2, H - 6);
            }
        },
        teardown: function () {
            if (this._keyHandler) {
                document.removeEventListener('keydown', this._keyHandler);
            }
        }
    };
})();
