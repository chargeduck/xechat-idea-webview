/**
 * 中国象棋渲染器。
 * 接收 Java 端推送的游戏状态，在 Canvas 上绘制棋盘和棋子。
 */
(function () {
    'use strict';

    // 棋盘常量
    const COLS = 9, ROWS = 10;
    const CELL = 56;          // 格子大小
    const PADDING = 36;       // 边距
    const PIECE_R = 24;       // 棋子半径
    const W = PADDING * 2 + CELL * (COLS - 1);
    const H = PADDING * 2 + CELL * (ROWS - 1);

    // 棋子 Unicode
    const PIECES = {
        red: { king: '帥', advisor: '仕', elephant: '相', horse: '馬', chariot: '車', cannon: '炮', pawn: '兵' },
        black: { king: '將', advisor: '士', elephant: '象', horse: '馬', chariot: '車', cannon: '砲', pawn: '卒' }
    };

    function drawBoard(ctx, w, h) {
        // 背景
        ctx.fillStyle = '#f0d9b5';
        ctx.fillRect(0, 0, w, h);

        // 网格线
        ctx.strokeStyle = '#333';
        ctx.lineWidth = 1;

        // 横线
        for (var r = 0; r < ROWS; r++) {
            var y = PADDING + r * CELL;
            ctx.beginPath();
            ctx.moveTo(PADDING, y);
            ctx.lineTo(PADDING + CELL * (COLS - 1), y);
            ctx.stroke();
        }

        // 竖线（楚河汉界处断开）
        for (var c = 0; c < COLS; c++) {
            var x = PADDING + c * CELL;
            // 上半部分
            ctx.beginPath();
            ctx.moveTo(x, PADDING);
            ctx.lineTo(x, PADDING + CELL * 4);
            ctx.stroke();
            // 下半部分
            ctx.beginPath();
            ctx.moveTo(x, PADDING + CELL * 5);
            ctx.lineTo(x, PADDING + CELL * 9);
            ctx.stroke();
        }

        // 边线加粗
        ctx.lineWidth = 2;
        ctx.strokeRect(PADDING, PADDING, CELL * (COLS - 1), CELL * (ROWS - 1));

        // 九宫格斜线
        ctx.lineWidth = 1;
        var drawPalace = function (topY) {
            ctx.beginPath();
            ctx.moveTo(PADDING + CELL * 3, topY);
            ctx.lineTo(PADDING + CELL * 5, topY + CELL * 2);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(PADDING + CELL * 5, topY);
            ctx.lineTo(PADDING + CELL * 3, topY + CELL * 2);
            ctx.stroke();
        };
        drawPalace(PADDING);
        drawPalace(PADDING + CELL * 7);

        // 楚河汉界
        ctx.fillStyle = '#333';
        ctx.font = 'bold 18px serif';
        ctx.textAlign = 'center';
        ctx.fillText('楚  河', PADDING + CELL * 2, PADDING + CELL * 4.6);
        ctx.fillText('汉  界', PADDING + CELL * 6, PADDING + CELL * 4.6);
    }

    function drawPiece(ctx, piece, selected, lastMove) {
        if (!piece) return;
        var x = PADDING + piece.col * CELL;
        var y = PADDING + piece.row * CELL;

        // 选中高亮
        if (selected && selected.col === piece.col && selected.row === piece.row) {
            ctx.fillStyle = 'rgba(255, 255, 0, 0.4)';
            ctx.beginPath();
            ctx.arc(x, y, PIECE_R + 4, 0, Math.PI * 2);
            ctx.fill();
        }

        // 最后一步高亮
        if (lastMove) {
            var from = lastMove.from, to = lastMove.to;
            var isFrom = from && from.col === piece.col && from.row === piece.row;
            var isTo = to && to.col === piece.col && to.row === piece.row;
            if (isFrom || isTo) {
                ctx.fillStyle = 'rgba(0, 200, 0, 0.3)';
                ctx.beginPath();
                ctx.arc(x, y, PIECE_R + 2, 0, Math.PI * 2);
                ctx.fill();
            }
        }

        // 棋子背景
        ctx.fillStyle = piece.side === 'red' ? '#fff5f5' : '#f5f5f5';
        ctx.beginPath();
        ctx.arc(x, y, PIECE_R, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = piece.side === 'red' ? '#c0392b' : '#2c3e50';
        ctx.lineWidth = 2;
        ctx.stroke();

        // 棋子文字
        ctx.fillStyle = piece.side === 'red' ? '#c0392b' : '#2c3e50';
        ctx.font = 'bold 22px "KaiTi", "楷体", serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(PIECES[piece.side][piece.type] || piece.type, x, y);
    }

    function drawHint(ctx, hint) {
        if (!hint) return;
        hint.forEach(function (p) {
            var x = PADDING + p.col * CELL;
            var y = PADDING + p.row * CELL;
            ctx.fillStyle = 'rgba(0, 180, 0, 0.3)';
            ctx.fillRect(x - PIECE_R * 0.5, y - PIECE_R * 0.5, PIECE_R, PIECE_R);
        });
    }

    function handleClick(e, canvas, gameState, callback) {
        var rect = canvas.getBoundingClientRect();
        var mx = e.clientX - rect.left;
        var my = e.clientY - rect.top;
        var col = Math.round((mx - PADDING) / CELL);
        var row = Math.round((my - PADDING) / CELL);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return;
        callback({ col: col, row: row });
    }

    // 注册到全局
    window.xechatGames = window.xechatGames || {};
    window.xechatGames[0] = {
        name: '中国象棋',
        canvasW: W,
        canvasH: H,
        setup: function (canvas, emitAction) {
            var self = this;
            self._emit = emitAction;
            self._gameState = null;

            self._clickHandler = function (e) {
                if (!self._gameState) return;
                handleClick(e, canvas, self._gameState, function (pos) {
                    emitAction(JSON.stringify({ action: 'click', pos: pos }));
                });
            };
            canvas.addEventListener('click', self._clickHandler);
        },
        render: function (canvas, gameState) {
            var ctx = canvas.getContext('2d');
            canvas.width = W;
            canvas.height = H;
            this._gameState = gameState;

            drawBoard(ctx, W, H);

            if (!gameState) return;

            // 绘制可走位置提示
            drawHint(ctx, gameState.hints);

            // 绘制棋子
            var pieces = gameState.pieces || [];
            pieces.forEach(function (p) {
                drawPiece(ctx, p, gameState.selected, gameState.lastMove);
            });

            // 信息栏
            if (gameState.message) {
                ctx.fillStyle = '#333';
                ctx.font = '14px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(gameState.message, W / 2, H - 8);
            }
        },
        teardown: function (canvas) {
            if (this._clickHandler) {
                canvas.removeEventListener('click', this._clickHandler);
            }
            this._gameState = null;
        }
    };
})();
