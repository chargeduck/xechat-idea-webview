/**
 * 数独渲染器。
 */
(function () {
    'use strict';

    var SIZE = 9;
    var CELL = 48;
    var W = CELL * SIZE;
    var H = CELL * SIZE + 30;

    function drawBoard(ctx, grid, selected, original) {
        ctx.fillStyle = '#f5f0e8';
        ctx.fillRect(0, 0, W, H);

        for (var r = 0; r < SIZE; r++) {
            for (var c = 0; c < SIZE; c++) {
                var x = c * CELL, y = r * CELL;
                var v = grid ? grid[r][c] : 0;

                // 宫格背景
                var boxR = Math.floor(r / 3), boxC = Math.floor(c / 3);
                if ((boxR + boxC) % 2 === 0) {
                    ctx.fillStyle = '#faf6ee';
                } else {
                    ctx.fillStyle = '#f0e8d8';
                }
                ctx.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);

                // 选中高亮
                if (selected && selected.row === r && selected.col === c) {
                    ctx.fillStyle = 'rgba(52, 152, 219, 0.25)';
                    ctx.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
                }

                // 同行同列高亮
                if (selected && (selected.row === r || selected.col === c)) {
                    ctx.fillStyle = 'rgba(52, 152, 219, 0.08)';
                    ctx.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
                }

                // 数字
                if (v > 0) {
                    ctx.fillStyle = (original && original[r][c] > 0) ? '#2c3e50' : '#2980b9';
                    ctx.font = 'bold 22px sans-serif';
                    ctx.textAlign = 'center';
                    ctx.textBaseline = 'middle';
                    ctx.fillText(v, x + CELL / 2, y + CELL / 2);
                }
            }
        }

        // 粗线（宫格边界）
        ctx.strokeStyle = '#555';
        ctx.lineWidth = 2;
        for (var i = 0; i <= SIZE; i++) {
            var isThick = i % 3 === 0;
            ctx.lineWidth = isThick ? 2 : 1;
            ctx.beginPath();
            ctx.moveTo(0, i * CELL);
            ctx.lineTo(W, i * CELL);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(i * CELL, 0);
            ctx.lineTo(i * CELL, W);
            ctx.stroke();
        }
    }

    function handleClick(e, canvas, emit) {
        var rect = canvas.getBoundingClientRect();
        var mx = e.clientX - rect.left, my = e.clientY - rect.top;
        var col = Math.floor(mx / CELL), row = Math.floor(my / CELL);
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return;
        emit(JSON.stringify({ action: 'select', pos: { row: row, col: col } }));
    }

    function handleKey(e, emit) {
        var v = parseInt(e.key);
        if (v >= 1 && v <= 9) {
            emit(JSON.stringify({ action: 'input', value: v }));
        } else if (e.key === 'Backspace' || e.key === 'Delete' || e.key === '0') {
            emit(JSON.stringify({ action: 'input', value: 0 }));
        }
    }

    window.xechatGames = window.xechatGames || {};
    window.xechatGames[7] = {
        name: '数独',
        canvasW: W,
        canvasH: H,
        setup: function (canvas, emit) {
            var self = this;
            self._emit = emit;
            self._clickHandler = function (e) { handleClick(e, canvas, emit); };
            self._keyHandler = function (e) { handleKey(e, emit); };
            canvas.addEventListener('click', self._clickHandler);
            document.addEventListener('keydown', self._keyHandler);
        },
        render: function (canvas, state) {
            canvas.width = W;
            canvas.height = H;
            this.canvasW = W;
            this.canvasH = H;
            var ctx = canvas.getContext('2d');
            drawBoard(ctx, state ? state.grid : null, state ? state.selected : null, state ? state.original : null);

            if (state && state.message) {
                ctx.fillStyle = '#333';
                ctx.font = '14px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(state.message, W / 2, W + 22);
            }
        },
        teardown: function (canvas) {
            if (this._clickHandler) canvas.removeEventListener('click', this._clickHandler);
            if (this._keyHandler) document.removeEventListener('keydown', this._keyHandler);
        }
    };
})();
