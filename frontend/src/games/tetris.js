/**
 * 俄罗斯方块渲染器。
 * 10x20 网格，7种四格骨牌。
 */
(function () {
    'use strict';

    var COLS = 10, ROWS = 20;
    var CELL = 28;
    var PADDING = 4;
    var W = PADDING * 2 + CELL * COLS;
    var H = PADDING * 2 + CELL * ROWS;

    var COLORS = [
        null,              // 0: 空
        '#00f0f0', '#f0a000', '#a000f0', '#00f000', // I J L O
        '#f00000', '#0000f0', '#f0f000', '#a0a0a0'  // S T Z ghost
    ];

    function drawGrid(ctx, grid) {
        ctx.fillStyle = '#1a1a2e';
        ctx.fillRect(0, 0, W, H);

        for (var r = 0; r < ROWS; r++) {
            for (var c = 0; c < COLS; c++) {
                var v = grid[r][c];
                var x = PADDING + c * CELL;
                var y = PADDING + r * CELL;

                if (v > 0) {
                    ctx.fillStyle = COLORS[v] || '#fff';
                    ctx.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
                    // 高光
                    ctx.fillStyle = 'rgba(255,255,255,0.3)';
                    ctx.fillRect(x + 1, y + 1, CELL - 2, 3);
                    ctx.fillRect(x + 1, y + 1, 3, CELL - 2);
                } else {
                    ctx.fillStyle = '#111122';
                    ctx.fillRect(x, y, CELL, CELL);
                }
                // 网格线
                ctx.strokeStyle = 'rgba(255,255,255,0.03)';
                ctx.strokeRect(x, y, CELL, CELL);
            }
        }
    }

    function drawNext(ctx, nextPiece, nextType) {
        if (!nextPiece) return;
        var nx = W + 16, ny = 20;
        ctx.fillStyle = '#fff';
        ctx.font = '12px sans-serif';
        ctx.fillText('NEXT', nx, ny - 4);

        for (var i = 0; i < nextPiece.length; i++) {
            var block = nextPiece[i];
            var x = nx + block.c * CELL * 0.7;
            var y = ny + block.r * CELL * 0.7;
            ctx.fillStyle = COLORS[nextType] || '#fff';
            ctx.fillRect(x, y, CELL * 0.7 - 2, CELL * 0.7 - 2);
        }
    }

    function handleKey(e, emit) {
        var map = { ArrowLeft: 'left', ArrowRight: 'right', ArrowDown: 'down', ArrowUp: 'rotate',
                    a: 'left', d: 'right', s: 'down', w: 'rotate', ' ': 'drop' };
        var action = map[e.key];
        if (action) {
            e.preventDefault();
            emit(JSON.stringify({ action: action }));
        }
    }

    window.xechatGames = window.xechatGames || {};
    window.xechatGames[2] = {
        name: '俄罗斯方块',
        canvasW: W + 120,
        canvasH: H,
        setup: function (canvas, emit) {
            var self = this;
            self._emit = emit;
            self._keyHandler = function (e) { handleKey(e, emit); };
            document.addEventListener('keydown', self._keyHandler);
        },
        render: function (canvas, state) {
            var totalW = W + 120;
            canvas.width = totalW;
            canvas.height = H;
            this.canvasW = totalW;
            this.canvasH = H;

            var ctx = canvas.getContext('2d');
            if (state && state.grid) {
                drawGrid(ctx, state.grid);
            } else {
                drawGrid(ctx, emptyGrid());
            }

            if (state) {
                drawNext(ctx, state.nextPiece, state.nextType);

                ctx.fillStyle = '#aaa';
                ctx.font = '13px sans-serif';
                var nx = W + 16;
                ctx.fillText('Score: ' + (state.score || 0), nx, 180);
                ctx.fillText('Level: ' + (state.level || 0), nx, 200);
                ctx.fillText('Lines: ' + (state.lines || 0), nx, 220);

                if (state.gameOver) {
                    ctx.fillStyle = 'rgba(0,0,0,0.6)';
                    ctx.fillRect(0, H / 2 - 30, W, 60);
                    ctx.fillStyle = '#f00';
                    ctx.font = 'bold 24px sans-serif';
                    ctx.textAlign = 'center';
                    ctx.fillText('GAME OVER', W / 2, H / 2 + 8);
                    ctx.textAlign = 'start';
                }
            }
        },
        teardown: function () {
            if (this._keyHandler) {
                document.removeEventListener('keydown', this._keyHandler);
            }
        }
    };

    function emptyGrid() {
        var g = [];
        for (var r = 0; r < ROWS; r++) {
            g[r] = [];
            for (var c = 0; c < COLS; c++) g[r][c] = 0;
        }
        return g;
    }
})();
