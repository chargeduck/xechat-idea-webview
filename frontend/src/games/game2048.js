/**
 * 2048 渲染器。
 * 4x4 网格，DOM 渲染（滑动合并动画）。
 */
(function () {
    'use strict';

    var SIZE = 4;
    var CELL = 80;
    var GAP = 10;
    var PADDING = 12;
    var W = PADDING * 2 + CELL * SIZE + GAP * (SIZE - 1);

    var COLOR_MAP = {
        0:    { bg: '#cdc1b4', fg: '#cdc1b4' },
        2:    { bg: '#eee4da', fg: '#776e65' },
        4:    { bg: '#ede0c8', fg: '#776e65' },
        8:    { bg: '#f2b179', fg: '#f9f6f2' },
        16:   { bg: '#f59563', fg: '#f9f6f2' },
        32:   { bg: '#f67c5f', fg: '#f9f6f2' },
        64:   { bg: '#f65e3b', fg: '#f9f6f2' },
        128:  { bg: '#edcf72', fg: '#f9f6f2' },
        256:  { bg: '#edcc61', fg: '#f9f6f2' },
        512:  { bg: '#edc850', fg: '#f9f6f2' },
        1024: { bg: '#edc53f', fg: '#f9f6f2' },
        2048: { bg: '#edc22e', fg: '#f9f6f2' },
        4096: { bg: '#3c3a32', fg: '#f9f6f2' },
    };

    function handleKey(e, emit) {
        var map = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right',
                    w: 'up', s: 'down', a: 'left', d: 'right' };
        var dir = map[e.key];
        if (dir) {
            e.preventDefault();
            emit(JSON.stringify({ action: 'move', direction: dir }));
        }
    }

    window.xechatGames = window.xechatGames || {};
    window.xechatGames[8] = {
        name: '2048',
        canvasW: W,
        canvasH: W + 40,
        domRender: true,
        setup: function (canvas, emit) {
            var self = this;
            self._emit = emit;
            self._keyHandler = function (e) { handleKey(e, emit); };
            document.addEventListener('keydown', self._keyHandler);
        },
        render: function (canvas, state) {
            if (!state || !state.grid) return;
            var container = canvas.parentElement;
            // 确保 DOM 容器存在
            var el = container.querySelector('.game-2048-board');
            if (!el) {
                el = document.createElement('div');
                el.className = 'game-2048-board';
                el.style.cssText = 'width:' + W + 'px;margin:0 auto;position:relative;background:#bbada0;border-radius:6px;padding:' + PADDING + 'px;';
                container.appendChild(el);
                canvas.style.display = 'none';
            }

            var html = '';
            for (var r = 0; r < SIZE; r++) {
                for (var c = 0; c < SIZE; c++) {
                    var v = state.grid[r][c];
                    var color = COLOR_MAP[v] || COLOR_MAP[4096];
                    var left = PADDING + c * (CELL + GAP);
                    var top = PADDING + r * (CELL + GAP);
                    var fontSize = v < 100 ? '32px' : v < 1000 ? '28px' : '22px';
                    html += '<div style="position:absolute;left:' + left + 'px;top:' + top +
                        'px;width:' + CELL + 'px;height:' + CELL + 'px;background:' + color.bg +
                        ';border-radius:4px;display:flex;align-items:center;justify-content:center;' +
                        'font-size:' + fontSize + ';font-weight:bold;color:' + color.fg + ';' +
                        'transition:all 0.1s ease;">' + (v || '') + '</div>';
                }
            }
            el.innerHTML = html;

            // 分数
            var scoreEl = container.querySelector('.game-2048-score');
            if (!scoreEl) {
                scoreEl = document.createElement('div');
                scoreEl.className = 'game-2048-score';
                scoreEl.style.cssText = 'text-align:center;padding:8px 0;font-size:14px;color:var(--text-secondary);';
                container.appendChild(scoreEl);
            }
            scoreEl.textContent = 'Score: ' + (state.score || 0) + (state.best ? '  Best: ' + state.best : '');

            if (state.gameOver) {
                scoreEl.textContent += '  -  GAME OVER (按R重来)';
            }
        },
        teardown: function () {
            if (this._keyHandler) {
                document.removeEventListener('keydown', this._keyHandler);
            }
            var container = document.querySelector('.game-content');
            if (container) {
                var el = container.querySelector('.game-2048-board');
                if (el) el.remove();
                var scoreEl = container.querySelector('.game-2048-score');
                if (scoreEl) scoreEl.remove();
            }
        }
    };
})();
