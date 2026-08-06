/**
 * 嵌入式浏览器工具组件。
 * 通过 iframe 加载网页，支持地址栏导航。
 */
(function () {
    'use strict';

    window.xechatTools = window.xechatTools || {};
    window.xechatTools.browser = {
        name: '浏览器',
        setup: function (container, emit) {
            var html = '<div style="display:flex;flex-direction:column;height:100%;">' +
                '<div style="display:flex;padding:6px;gap:4px;background:var(--bg-secondary);border-bottom:1px solid var(--border-color);">' +
                '<input id="browser-url" type="text" style="flex:1;padding:4px 8px;border:1px solid var(--border-color);border-radius:3px;font-size:13px;" placeholder="输入网址后按回车..." />' +
                '<button id="browser-go" style="padding:4px 12px;" class="primary">前往</button>' +
                '</div>' +
                '<iframe id="browser-frame" style="flex:1;border:none;width:100%;" sandbox="allow-scripts allow-same-origin allow-forms allow-popups"></iframe>' +
                '</div>';
            container.innerHTML = html;

            var urlInput = container.querySelector('#browser-url');
            var goBtn = container.querySelector('#browser-go');
            var frame = container.querySelector('#browser-frame');

            function navigate() {
                var url = urlInput.value.trim();
                if (!url) return;
                if (!/^https?:\/\//i.test(url)) {
                    url = 'https://' + url;
                }
                frame.src = url;
                urlInput.value = url;
            }

            urlInput.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') navigate();
            });
            goBtn.addEventListener('click', navigate);

            // 默认加载空白页
            frame.src = 'about:blank';
        },
        teardown: function (container) {
            container.innerHTML = '';
        }
    };
})();
