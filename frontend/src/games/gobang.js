/**
 * 五子棋渲染器。
 * 15x15 棋盘，黑白轮流落子。
 */
(function () {
    'use strict';

    var SIZE = 15;
    var CELL = 36;
    var PADDING = 28;
    var PIECE_R = 15;
    var W = PADDING * 2 + CELL * (SIZE - 1);
    var H = W;

    function drawBoard(ctx) {
        ctx.fillStyle = '#deb887';
        ctx.fillRect(0, 0, W, H);

        ctx.strokeStyle = '#333';
        ctx.lineWidth = 1;
        for (var i = 0; i < SIZE; i++) {
            var pos = PADDING + i * CELL;
            ctx.beginPath();
            ctx.moveTo(PADDING, pos);
            ctx.lineTo(PADDING + CELL * (SIZE - 1), pos);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(pos, PADDING);
            ctx.lineTo(pos, PADDING + CELL * (SIZE - 1));
            ctx.stroke();
        }

        // 星位
        var stars = [[3,3],[3,7],[3,11],[7,3],[7,7],[7,11],[11,3],[11,7],[11,11]];
        ctx.fillStyle = '#333';
        stars.forEach(function (s) {
            ctx.beginPath();
            ctx.arc(PADDING + s[0] * CELL, PADDING + s[1] * CELL, 3, 0, Math.PI * 2);
            ctx.fill();
        });
    }

    function drawPiece(ctx, piece) {
        if (!piece) return;
        var x = PADDING + piece.col * CELL;
        var y = PADDING + piece.row * CELL;
        ctx.beginPath();
        ctx.arc(x, y, PIECE_R, 0, Math.PI * 2);
        var grad = ctx.createRadialGradient(x - 3, y - 3, 2, x, y, PIECE_R);
        if (piece.color === 'black') {
            grad.addColorStop(0, '#555');
            grad.addColorStop(1, '#111');
        } else {
            grad.addColorStop(0, '#fff');
            grad.addColorStop(1, '#ccc');
        }
        ctx.fillStyle = grad;
        ctx.fill();
        ctx.strokeStyle = '#333';
        ctx.lineWidth = 1;
        ctx.stroke();
    }

    function handleClick(e, canvas, emit) {
        var rect = canvas.getBoundingClientRect();
        var mx = e.clientX - rect.left;
        var my = e.clientY - rect.top;
        var col = Math.round((mx - PADDING) / CELL);
        var row = Math.round((my - PADDING) / CELL);
        if (col < 0 || col >= SIZE || row < 0 || row >= SIZE) return;
        emit(JSON.stringify({ action: 'place', pos: { col: col, row: row } }));
    }

    window.xechatGames = window.xechatGames || {};
    window.xechatGames[5] = {
        name: '五子棋',
        canvasW: W,
        canvasH: H + 30,
        setup: function (canvas, emit) {
            var self = this;
            self._emit = emit;
            self._clickHandler = function (e) { handleClick(e, canvas, emit); };
            canvas.addEventListener('click', self._clickHandler);
        },
        render: function (canvas, state) {
            canvas.width = W;
            canvas.height = H + 30;
            this.canvasW = W;
            this.canvasH = H + 30;

            var ctx = canvas.getContext('2d');
            drawBoard(ctx);

            if (!state) return;
            var pieces = state.pieces || [];
            pieces.forEach(function (p) { drawPiece(ctx, p); });

            // 最后一步标记
            if (state.lastPos) {
                var lx = PADDING + state.lastPos.col * CELL;
                var ly = PADDING + state.lastPos.row * CELL;
                ctx.fillStyle = '#e74c3c';
                ctx.beginPath();
                ctx.arc(lx, ly, 4, 0, Math.PI * 2);
                ctx.fill();
            }

            ctx.fillStyle = '#333';
            ctx.font = '14px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText(state.message || (state.currentPlayer || '黑') + '方走棋', W / 2, H + 20);

            if (state.gameOver) {
                ctx.fillStyle = 'rgba(0,0,0,0.4)';
                ctx.fillRect(0, H / 2 - 25, W, 50);
                ctx.fillStyle = '#fff';
                ctx.font = 'bold 22px sans-serif';
                ctx.fillText(state.message, W / 2, H / 2 + 8);
            }
        },
        teardown: function (canvas) {
            if (this._clickHandler) {
                canvas.removeEventListener('click', this._clickHandler);
            }
        }
    };
})();
