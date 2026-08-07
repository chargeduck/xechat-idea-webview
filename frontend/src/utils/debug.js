/**
 * JCEF 调试工具：捕获 console.log/error/warn 到页面浮层
 * 在 JCEF 无 DevTools 环境下替代浏览器控制台。
 */
;(function() {
    'use strict'

    var MAX_LINES = 200
    var panel = null
    var logLines = []

    function createPanel() {
        panel = document.createElement('div')
        panel.id = '__jcef_debug'
        panel.style.cssText = [
            'position:fixed;top:0;right:0;width:30vw;height:100vh;z-index:99999;',
            'background:rgba(0,0,0,0.92);color:#0f0;font:11px/1.4 "Courier New",monospace;',
            'overflow-y:auto;padding:6px 10px;box-sizing:border-box;',
            'display:none;white-space:pre-wrap;word-break:break-all;',
            'border-left:2px solid #333;'
        ].join('')
        document.body.appendChild(panel)
    }

    function appendLog(level, args) {
        var now = new Date()
        var ts = now.toTimeString().slice(0,8) + '.' + String(now.getMilliseconds()).padStart(3,'0')
        var text = Array.from(args).map(function(a) {
            if (a instanceof Error) return a.stack || a.message
            if (typeof a === 'object') {
                try { return JSON.stringify(a).slice(0,500) } catch(e) {}
            }
            return String(a).slice(0,500)
        }).join(' ')
        var line = '[' + ts + '][' + level + '] ' + text
        logLines.push(line)
        if (logLines.length > MAX_LINES) logLines.shift()
        if (panel) {
            panel.textContent = logLines.join('\n')
            panel.scrollTop = panel.scrollHeight
        }
    }

    // 劫持 console
    var _orig = { log: console.log, error: console.error, warn: console.warn }
    ;['log','error','warn'].forEach(function(level) {
        console[level] = function() {
            _orig[level].apply(console, arguments)
            appendLog(level.toUpperCase(), arguments)
        }
    })

    // 全局错误也进调试面板
    window.addEventListener('error', function(e) {
        if (e.target === window || e.target === document) {
            appendLog('ERROR', [e.message || 'Script error', e.filename, e.lineno + ':' + e.colno])
        }
    }, true)
    window.addEventListener('unhandledrejection', function(e) {
        appendLog('REJECT', [String(e.reason)])
    })

    // 快捷键切换：Ctrl+Shift+D
    document.addEventListener('keydown', function(e) {
        if (e.ctrlKey && e.shiftKey && e.key === 'D') {
            if (!panel) createPanel()
            panel.style.display = panel.style.display === 'none' ? 'block' : 'none'
        }
    })

    // 创建面板
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', createPanel)
    } else {
        createPanel()
    }
})()
