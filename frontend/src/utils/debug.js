/**
 * JCEF 调试工具 v3：捕获 console 到浮层 + 操作快照(200条) + 错误上下文回溯 + 全局埋点
 * Ctrl+Shift+D 切换调试面板
 */
;(function() {
    'use strict'

    var MAX_LINES = 200
    var MAX_SNAPSHOTS = 200
    var panel = null
    var logLines = []
    var snapshots = []

    function ts() {
        var d = new Date()
        return d.toTimeString().slice(0,8) + '.' + String(d.getMilliseconds()).padStart(3,'0')
    }

    function createPanel() {
        panel = document.createElement('div')
        panel.id = '__jcef_debug'
        panel.style.cssText = [
            'position:fixed;top:0;right:0;width:35vw;height:100vh;z-index:99999;',
            'background:rgba(0,0,0,0.93);color:#0f0;font:10px/1.35 "Courier New",monospace;',
            'overflow-y:auto;padding:6px 10px;box-sizing:border-box;',
            'display:none;white-space:pre-wrap;word-break:break-all;',
            'border-left:2px solid #333;'
        ].join('')
        document.body.appendChild(panel)
    }

    function pushSnapshot(tag, detail) {
        // 过滤 debug 面板自身操作，避免噪音
        if (tag === 'scrollTop' || tag === 'scrollLeft') {
            var isPanel = String(detail || '').indexOf('__jcef_debug') !== -1
            if (isPanel) return
        }
        var stack = ''
        try { stack = new Error().stack || '' } catch(e) {}
        var lines = stack.split('\n').slice(2, 5).map(function(s) { return s.trim() }).join(' <- ')
        snapshots.push({ t: ts(), tag: tag, detail: String(detail || '').slice(0, 200), stack: lines })
        if (snapshots.length > MAX_SNAPSHOTS) snapshots.shift()
    }

    // 暴露全局埋点接口
    window.__debug_pushSnapshot = pushSnapshot

    function appendLog(level, args) {
        var text = Array.from(args).map(function(a) {
            if (a instanceof Error) return a.stack || a.message
            if (typeof a === 'object') {
                try { return JSON.stringify(a).slice(0,500) } catch(e) {}
            }
            return String(a).slice(0,500)
        }).join(' ')
        var line = '[' + ts() + '][' + level + '] ' + text
        logLines.push(line)
        if (logLines.length > MAX_LINES) logLines.shift()
        if (panel) {
            panel.textContent = logLines.join('\n')
            panel.scrollTop = panel.scrollHeight
        }
    }

    // ============================================================
    // 1. 劫持 console
    // ============================================================
    var _orig = { log: console.log, error: console.error, warn: console.warn }
    ;['log','error','warn'].forEach(function(level) {
        console[level] = function() {
            _orig[level].apply(console, arguments)
            appendLog(level.toUpperCase(), arguments)
        }
    })

    // ============================================================
    // 2. 拦截关键 DOM API
    // ============================================================
    var _lastInnerHTML = ''  // 最近一次 innerHTML 写入（用于 ONERROR dump）
    ;(function() {
        var _innerHTML = Object.getOwnPropertyDescriptor(Element.prototype, 'innerHTML')
        if (_innerHTML && _innerHTML.set) {
            var _setInner = _innerHTML.set
            Object.defineProperty(Element.prototype, 'innerHTML', {
                set: function(val) {
                    var tag = this.tagName || '?'
                    var cls = this.className || ''
                    var len = typeof val === 'string' ? val.length : 0
                    _lastInnerHTML = String(val || '').slice(0, 600)
                    pushSnapshot('innerHTML', tag + '.' + cls + ' len=' + len)
                    _setInner.call(this, val)
                },
                get: _innerHTML.get,
                configurable: true
            })
        }

        ;['scrollTop','scrollLeft'].forEach(function(prop) {
            var desc = Object.getOwnPropertyDescriptor(Element.prototype, prop)
            if (desc && desc.set) {
                var _set = desc.set
                Object.defineProperty(Element.prototype, prop, {
                    set: function(val) {
                        pushSnapshot(prop, (this.className||this.tagName||'?') + '=' + val)
                        _set.call(this, val)
                    },
                    get: desc.get,
                    configurable: true
                })
            }
        })

        var _appendChild = Node.prototype.appendChild
        Node.prototype.appendChild = function(child) {
            pushSnapshot('appendChild', (this.className||this.tagName||'?') + ' <- ' + ((child&&child.tagName)||'?'))
            return _appendChild.call(this, child)
        }

        var _insertBefore = Node.prototype.insertBefore
        Node.prototype.insertBefore = function(newNode, refNode) {
            pushSnapshot('insertBefore', (this.className||this.tagName||'?') + ' <- ' + ((newNode&&newNode.tagName)||'?'))
            return _insertBefore.call(this, newNode, refNode)
        }

        var _removeChild = Node.prototype.removeChild
        Node.prototype.removeChild = function(child) {
            pushSnapshot('removeChild', (this.className||this.tagName||'?') + ' - ' + ((child&&child.tagName)||'?'))
            return _removeChild.call(this, child)
        }

        // Vue 3 使用的现代 DOM API（Element 级别，非 Node 级别）
        if (Element.prototype.append) {
            var _append = Element.prototype.append
            Element.prototype.append = function() {
                pushSnapshot('append', (this.className||this.tagName||'?') + ' +' + arguments.length + ' nodes')
                return _append.apply(this, arguments)
            }
        }
        if (Element.prototype.prepend) {
            var _prepend = Element.prototype.prepend
            Element.prototype.prepend = function() {
                pushSnapshot('prepend', (this.className||this.tagName||'?') + ' +' + arguments.length + ' nodes')
                return _prepend.apply(this, arguments)
            }
        }
        if (Element.prototype.before) {
            var _before = Element.prototype.before
            Element.prototype.before = function() {
                pushSnapshot('before', (this.className||this.tagName||'?') + ' +' + arguments.length + ' nodes')
                return _before.apply(this, arguments)
            }
        }
        if (Element.prototype.after) {
            var _after = Element.prototype.after
            Element.prototype.after = function() {
                pushSnapshot('after', (this.className||this.tagName||'?') + ' +' + arguments.length + ' nodes')
                return _after.apply(this, arguments)
            }
        }
        if (Element.prototype.remove) {
            var _remove = Element.prototype.remove
            Element.prototype.remove = function() {
                pushSnapshot('remove', (this.className||this.tagName||'?'))
                return _remove.call(this)
            }
        }
        if (Element.prototype.replaceWith) {
            var _replaceWith = Element.prototype.replaceWith
            Element.prototype.replaceWith = function() {
                pushSnapshot('replaceWith', (this.className||this.tagName||'?'))
                return _replaceWith.apply(this, arguments)
            }
        }
        if (Element.prototype.replaceChildren) {
            var _replaceChildren = Element.prototype.replaceChildren
            Element.prototype.replaceChildren = function() {
                pushSnapshot('replaceChildren', (this.className||this.tagName||'?') + ' +' + arguments.length + ' nodes')
                return _replaceChildren.apply(this, arguments)
            }
        }
    })()

    // ============================================================
    // 3. 包装异步回调，捕获内部错误
    // ============================================================
    ;(function() {
        var _setTimeout = window.setTimeout
        window.setTimeout = function(fn, delay) {
            var args = Array.prototype.slice.call(arguments, 2)
            return _setTimeout(function() {
                try { return fn.apply(this, args) }
                catch(e) { appendLog('TIMEOUT_ERR', [String(e), e.stack||'']) }
            }, delay)
        }

        var _setInterval = window.setInterval
        window.setInterval = function(fn, delay) {
            var args = Array.prototype.slice.call(arguments, 2)
            return _setInterval(function() {
                try { return fn.apply(this, args) }
                catch(e) { appendLog('INTERVAL_ERR', [String(e), e.stack||'']) }
            }, delay)
        }

        if (window.requestAnimationFrame) {
            var _raf = window.requestAnimationFrame
            window.requestAnimationFrame = function(fn) {
                return _raf(function(t) {
                    try { return fn(t) }
                    catch(e) { appendLog('RAF_ERR', [String(e), e.stack||'']) }
                })
            }
        }

        if (window.MutationObserver) {
            var _MO = window.MutationObserver
            window.MutationObserver = function(callback) {
                return new _MO(function(mutations, observer) {
                    pushSnapshot('MutationObserver', mutations.length + ' mutations')
                    try { return callback(mutations, observer) }
                    catch(e) { appendLog('MO_ERR', [String(e), e.stack||'']) }
                })
            }
            window.MutationObserver.prototype = _MO.prototype
        }

        if (window.ResizeObserver) {
            var _RO = window.ResizeObserver
            window.ResizeObserver = function(callback) {
                return new _RO(function(entries, observer) {
                    try { return callback(entries, observer) }
                    catch(e) { appendLog('RO_ERR', [String(e), e.stack||'(no stack)']) }
                })
            }
            window.ResizeObserver.prototype = _RO.prototype
        }
    })()

    // ============================================================
    // 4. EventTarget.dispatchEvent 拦截
    // ============================================================
    ;(function() {
        var _dispatch = EventTarget.prototype.dispatchEvent
        EventTarget.prototype.dispatchEvent = function(event) {
            var type = event && event.type
            if (type && type.indexOf('mouse') === -1 && type.indexOf('pointer') === -1 && type.indexOf('touch') === -1) {
                pushSnapshot('event', type)
            }
            try {
                return _dispatch.call(this, event)
            } catch(e) {
                appendLog('EVENT_ERR', [type, String(e), e.stack||''])
                throw e
            }
        }
    })()

    // ============================================================
    // 5. 全局错误 + 快照 dump
    // ============================================================
    window.addEventListener('error', function(e) {
        if (e.target === window || e.target === document) {
            var msg = e.message || 'Script error'
            var loc = (e.filename||'') + ' ' + (e.lineno||'') + ':' + (e.colno||'')

            var recent = snapshots.slice(-50)
            var snapDump = recent.map(function(s) {
                return '  [' + s.t + '] ' + s.tag + ': ' + s.detail + '\n    <- ' + s.stack
            }).join('\n')

            var errInfo = msg + ' @ ' + loc
            if (e.error) {
                if (e.error.name) errInfo += ' name=' + e.error.name
                if (e.error.message) errInfo += ' msg=' + e.error.message
                if (e.error.stack) errInfo += '\nSTACK:' + e.error.stack.slice(0, 800)
            }
            errInfo += '\n--- last rendered HTML ---\n' + (window.__lastRenderedHtml || '(none)')

            appendLog('ONERROR', [errInfo + '\n--- snapshots ---\n' + (snapDump || '  (none)')])
            _orig.error.call(console, '[ONERROR]', errInfo)
            if (snapDump) _orig.error.call(console, '[ONERROR snapshots]\n' + snapDump)
        }
    }, true)

    window.addEventListener('unhandledrejection', function(e) {
        appendLog('REJECT', [String(e.reason), (e.reason&&e.reason.stack||'')])
    })

    // Ctrl+Shift+D 切换面板
    document.addEventListener('keydown', function(e) {
        if (e.ctrlKey && e.shiftKey && e.key === 'D') {
            if (!panel) createPanel()
            panel.style.display = panel.style.display === 'none' ? 'block' : 'none'
        }
    })

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', createPanel)
    } else {
        createPanel()
    }
})()
