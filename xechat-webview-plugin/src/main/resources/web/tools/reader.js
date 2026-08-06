/**
 * 阅读器工具组件。
 * 渲染纯文本 / Markdown 内容，通过 JSBridge 获取内容。
 */
(function () {
    'use strict';

    // 简易 Markdown 渲染（仅支持标题、粗体、斜体、代码、列表）
    function renderMarkdown(text) {
        if (!text) return '';
        var html = text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');

        // 代码块
        html = html.replace(/```(\w*)\n([\s\S]*?)```/g, function (_, lang, code) {
            return '<pre style="background:#1e1e1e;color:#d4d4d4;padding:12px;border-radius:4px;overflow-x:auto;font-size:13px;"><code>' + code.trim() + '</code></pre>';
        });

        // 标题
        html = html.replace(/^#### (.+)$/gm, '<h4 style="margin:12px 0 4px;font-size:15px;">$1</h4>');
        html = html.replace(/^### (.+)$/gm, '<h3 style="margin:14px 0 4px;font-size:17px;">$1</h3>');
        html = html.replace(/^## (.+)$/gm, '<h2 style="margin:16px 0 6px;font-size:19px;">$1</h2>');
        html = html.replace(/^# (.+)$/gm, '<h1 style="margin:18px 0 8px;font-size:22px;">$1</h1>');

        // 粗体/斜体
        html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');

        // 无序列表
        html = html.replace(/^- (.+)$/gm, '<li style="margin-left:20px;">$1</li>');
        html = html.replace(/((?:<li[^>]*>.*<\/li>\n?)+)/g, '<ul style="margin:4px 0;">$1</ul>');

        // 行内代码
        html = html.replace(/`(.+?)`/g, '<code style="background:#e8e8e8;padding:1px 4px;border-radius:2px;font-size:13px;">$1</code>');

        // 段落
        html = html.replace(/\n\n/g, '</p><p style="margin:8px 0;line-height:1.7;">');
        html = '<p style="margin:8px 0;line-height:1.7;">' + html + '</p>';

        // 单换行
        html = html.replace(/\n/g, '<br>');

        return html;
    }

    window.xechatTools = window.xechatTools || {};
    window.xechatTools.reader = {
        name: '阅读器',
        setup: function (container, emit) {
            var html = '<div style="height:100%;overflow-y:auto;padding:16px 20px;font-size:15px;line-height:1.8;">' +
                '<div id="reader-content" style="max-width:800px;margin:0 auto;">' +
                '<div style="color:var(--text-muted);text-align:center;padding:40px;">正在加载...</div>' +
                '</div></div>';
            container.innerHTML = html;

            // 请求 Java 端返回内容
            emit(JSON.stringify({ action: 'load' }));
        },
        render: function (container, content) {
            var el = container.querySelector('#reader-content');
            if (el && content) {
                el.innerHTML = renderMarkdown(content.text || content);
            }
        },
        teardown: function (container) {
            container.innerHTML = '';
        }
    };
})();
