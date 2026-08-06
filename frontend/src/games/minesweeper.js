/**
 * 扫雷渲染器。
 */
(function () {
    'use strict';

    var DIFFICULTY = { beginner: [9, 9, 10], intermediate: [16, 16, 40], expert: [30, 16, 99] };

    function cellSize(rows, cols) { return Math.min(32, Math.floor(500 / Math.max(rows, cols))); }

    function draw(ctx, state) {
        if (!state || !state.grid) return;
        var grid = state.grid;
        var rows = grid.length, cols = grid[0].length;
        var cs = cellSize(rows, cols);
        var W = cols * cs, H = rows * cs;

        for (var r = 0; r < rows; r++) {
            for (var c = 0; c < cols; c++) {
                var cell = grid[r][c];
                var x = c * cs, y = r * cs;

                if (cell.revealed) {
                    ctx.fillStyle = '#d4c4a8';
                    ctx.fillRect(x, y, cs, cs);
                    ctx.strokeStyle = '#bbb';
                    ctx.strokeRect(x, y, cs, cs);

                    if (cell.mine) {
                        ctx.fillStyle = '#e74c3c';
                        ctx.beginPath();
                        ctx.arc(x + cs / 2, y + cs / 2, cs * 0.35, 0, Math.PI * 2);
                        ctx.fill();
                        ctx.fillStyle = '#000';
                        ctx.font = 'bold ' + cs * 0.5 + 'px sans-serif';
                        ctx.textAlign = 'center';
                        ctx.textBaseline = 'middle';
                        ctx.fillText('*', x + cs / 2, y + cs / 2);
                    } else if (cell.adjacent > 0) {
                        var colors = ['', '#00f', '#080', '#e00', '#008', '#800', '#088', '#000', '#888'];
                        ctx.fillStyle = colors[cell.adjacent] || '#000';
                        ctx.font = 'bold ' + cs * 0.55 + 'px sans-serif';
                        ctx.textAlign = 'center';
                        ctx.textBaseline = 'middle';
                        ctx.fillText(cell.adjacent, x + cs / 2, y + cs / 2);
                    }
                } else {
                    // 未翻开
                    ctx.fillStyle = '#8b9dc3';
                    ctx.fillRect(x + 1, y + 1, cs - 2, cs - 2);
                    // 3D 效果
                    ctx.fillStyle = 'rgba(255,255,255,0.4)';
                    ctx.fillRect(x + 1, y + 1, cs - 2, 2);
                    ctx.fillRect(x + 1, y + 1, 2, cs - 2);
                    ctx.fillStyle = 'rgba(0,0,0,0.3)';
                    ctx.fillRect(x + 1, y + cs - 3, cs - 2, 2);
                    ctx.fillRect(x + cs - 3, y + 1, 2, cs - 2);

                    if (cell.flagged) {
                        ctx.fillStyle = '#e74c3c';
                        ctx.font = 'bold ' + cs * 0.5 + 'px sans-serif';
                        ctx.textAlign = 'center';
                        ctx.textBaseline = 'middle';
                        ctx.fillText('⚑', x + cs / 2, y + cs / 2);
                    }
                }
            }
        }

        // 面板
        if (state.message) {
            ctx.fillStyle = '#555';
            ctx.font = '13px sans-serif';
            ctx.textAlign = 'left';
            ctx.fillText('💣 ' + (state.remaining || state.mines || 0) + '  ' + state.message, 8, H - 6);
        }
    }

    function handleClick(e, canvas, emit) {
        var rect = canvas.getBoundingClientRect();
        var mx = e.clientX - rect.left, my = e.clientY - rect.top;
        var state = this._state;
        if (!state || !state.grid) return;
        var rows = state.grid.length, cols = state.grid[0].length;
        var cs = cellSize(rows, cols);
        var c = Math.floor(mx / cs), r = Math.floor(my / cs);
        if (r < 0 || r >= rows || c < 0 || c >= cols) return;

        if (e.type === 'contextmenu' || e.button === 2) {
            e.preventDefault();
            emit(JSON.stringify({ action: 'flag', pos: { row: r, col: c } }));
        } else {
            emit(JSON.stringify({ action: 'reveal', pos: { row: r, col: c } }));
        }
    }

    window.xechatGames = window.xechatGames || {};
    window.xechatGames[3] = {
        name: '扫雷',
        canvasW: 500,
        canvasH: 500,
        setup: function (canvas, emit) {
            var self = this;
            self._emit = emit;
            self._clickHandler = function (e) { handleClick.call(self, e, canvas, emit); };
            self._ctxMenu = function (e) { handleClick.call(self, e, canvas, emit); };
            canvas.addEventListener('click', self._clickHandler);
            canvas.addEventListener('contextmenu', self._ctxMenu);
        },
        render: function (canvas, state) {
            this._state = state;
            if (!state || !state.grid) return;
            var rows = state.grid.length, cols = state.grid[0].length;
            var cs = cellSize(rows, cols);
            var W = cols * cs, H = rows * cs + 24;
            canvas.width = W;
            canvas.height = H;
            this.canvasW = W;
            this.canvasH = H;
            draw(canvas.getContext('2d'), state);
        },
        teardown: function (canvas) {
            if (this._clickHandler) canvas.removeEventListener('click', this._clickHandler);
            if (this._ctxMenu) canvas.removeEventListener('contextmenu', this._ctxMenu);
        }
    };
})();
