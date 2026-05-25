package com.webtoapp.core.webview

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger

/**
 * 网页颜色主题桥接器，通过 addJavascriptInterface 注入 WebView，
 * 实现 Java ↔ JS 双向通信。
 *
 * JS 端调用 _ColorThemeBridge.onColorExtracted(color) 报告提取到的网页颜色，
 * Java 端通过 onColorChanged 回调将颜色传递给 ShellScreen 应用到状态栏。
 *
 * 借鉴自 X浏览器的 color_theme / _COLOR_THEME_ 双向通信方案。
 */
class ColorThemeBridge(
    private val onColorChanged: (color: String) -> Unit,
    private val onColorCleared: () -> Unit = {}
) {
    companion object {
        const val JS_INTERFACE_NAME = "_ColorThemeBridge"

        /**
         * 注入到网页中的颜色提取脚本。
         * 优先级：
         * 1. 顶部 fixed/sticky 导航栏
         * 2. meta theme-color（近黑色降权；搜狐等站点首屏 meta 比 body 白底更可靠）
         * 3. 顶部可见区域采样（近白色降权，避免盖住 meta）
         * 4. body / html 背景
         *
         * 颜色通过 _ColorThemeBridge.onColorExtracted(color) 回传 Java 层。
         */
        val COLOR_EXTRACT_JS = """
(function() {
    var _wtc = window._wtc || {};
    window._wtc = _wtc;
    if (_wtc._extracting) return;
    _wtc._extracting = true;

    function toHex(color) {
        if (!color || color === 'transparent' || color === 'rgba(0, 0, 0, 0)') return null;
        if (color.startsWith('#')) {
            var h = color.substring(1);
            if (h.length === 3) {
                return '#' + h.split('').map(function(c) { return c + c; }).join('');
            }
            if (h.length >= 6) return '#' + h.substring(0, 6);
        }
        var match = color.match(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/);
        if (match) {
            return '#' + [match[1], match[2], match[3]]
                .map(function(x) { var n = parseInt(x).toString(16); return n.length === 1 ? '0' + n : n; })
                .join('');
        }
        return null;
    }

    function isNearBlack(hex) {
        if (!hex) return false;
        var h = hex.replace('#', '');
        if (h.length !== 6) return false;
        var r = parseInt(h.substring(0, 2), 16);
        var g = parseInt(h.substring(2, 4), 16);
        var b = parseInt(h.substring(4, 6), 16);
        return r < 24 && g < 24 && b < 24;
    }

    function isNearWhite(hex) {
        if (!hex) return false;
        var h = hex.replace('#', '');
        if (h.length !== 6) return false;
        var r = parseInt(h.substring(0, 2), 16);
        var g = parseInt(h.substring(2, 4), 16);
        var b = parseInt(h.substring(4, 6), 16);
        return r >= 240 && g >= 240 && b >= 240;
    }

    function pickBest(candidates) {
        var nonBlack = null;
        var any = null;
        for (var i = 0; i < candidates.length; i++) {
            var c = candidates[i];
            if (!c) continue;
            any = any || c;
            if (!isNearBlack(c)) return c;
            nonBlack = nonBlack || c;
        }
        return any;
    }

    function pickBestTopSample(candidates) {
        var nonWhite = null;
        var any = null;
        for (var i = 0; i < candidates.length; i++) {
            var c = candidates[i];
            if (!c) continue;
            any = any || c;
            if (!isNearWhite(c) && !isNearBlack(c)) return c;
            if (!isNearWhite(c)) nonWhite = nonWhite || c;
        }
        return nonWhite || any;
    }

    function extractFromFixedHeader() {
        try {
            var colors = [];
            var nodes = document.querySelectorAll('header, nav, [role="banner"], body *');
            for (var i = 0; i < nodes.length && i < 350; i++) {
                var el = nodes[i];
                var style = window.getComputedStyle(el);
                var pos = style.position;
                var isPinned = pos === 'fixed' || pos === 'sticky' || pos === '-webkit-sticky';
                var rect = el.getBoundingClientRect();
                if (rect.height < 16 || rect.bottom <= 0 || rect.top > 56) continue;
                var tag = (el.tagName || '').toLowerCase();
                var isBarLike = tag === 'header' || tag === 'nav' || (el.getAttribute && el.getAttribute('role') === 'banner');
                if (!isBarLike) {
                    var cls = (el.className && el.className.toString().toLowerCase()) || '';
                    var id = (el.id && el.id.toLowerCase()) || '';
                    isBarLike = /header|navbar|nav-bar|topbar|top-bar|app-bar|toolbar|search/.test(cls + ' ' + id);
                }
                if (!isPinned && !isBarLike) continue;
                if (isPinned && rect.width < window.innerWidth * 0.35 && !isBarLike) continue;
                var bg = toHex(style.backgroundColor);
                if (bg) colors.push(bg);
            }
            return pickBest(colors);
        } catch (e) {}
        return null;
    }

    function extractFromTopVisibleArea() {
        try {
            var colors = [];
            if (document.elementsFromPoint) {
                var sampleXs = [0.15, 0.5, 0.85];
                var sampleYs = [8, 22, 40];
                for (var xi = 0; xi < sampleXs.length; xi++) {
                    for (var yi = 0; yi < sampleYs.length; yi++) {
                        var x = Math.max(0, Math.min(window.innerWidth - 1, Math.floor(window.innerWidth * sampleXs[xi])));
                        var y = sampleYs[yi];
                        var stack = document.elementsFromPoint(x, y) || [];
                        for (var i = 0; i < stack.length && i < 12; i++) {
                            var el = stack[i];
                            if (el === document.documentElement || el === document.body) continue;
                            var bg = toHex(window.getComputedStyle(el).backgroundColor);
                            if (bg) colors.push(bg);
                        }
                    }
                }
            }
            var picked = pickBestTopSample(colors);
            if (picked) return picked;

            var elements = document.querySelectorAll('body *');
            for (var j = 0; j < elements.length && j < 500; j++) {
                var node = elements[j];
                var rect = node.getBoundingClientRect();
                if (rect.height <= 0 || rect.bottom < 0 || rect.top > 72) continue;
                var elBg = toHex(window.getComputedStyle(node).backgroundColor);
                if (elBg) colors.push(elBg);
            }
            return pickBestTopSample(colors);
        } catch (e) {}
        return null;
    }

    function extractMetaColor() {
        var meta = document.querySelector('meta[name="theme-color"]');
        if (!meta) return null;
        var content = meta.getAttribute('content') || meta.getAttribute('value');
        return content ? toHex(content) : null;
    }

    function extractColor() {
        try {
            var headerColor = extractFromFixedHeader();
            var topColor = extractFromTopVisibleArea();
            var metaColor = extractMetaColor();
            // 顶栏子元素（搜索框等）可能是近白，不应压过 meta 品牌色
            if (headerColor && !isNearWhite(headerColor)) return headerColor;
            if (metaColor && !isNearBlack(metaColor)) return metaColor;
            if (headerColor) return headerColor;
            if (topColor && !isNearWhite(topColor)) return topColor;
            if (metaColor) return metaColor;
            return (function() {
                var body = document.body;
                if (body) {
                    var bg = toHex(window.getComputedStyle(body).backgroundColor);
                    if (bg) return bg;
                }
                var html = document.documentElement;
                if (html) {
                    var htmlBg = toHex(window.getComputedStyle(html).backgroundColor);
                    if (htmlBg) return htmlBg;
                }
                return null;
            })();
        } catch (e) {}
        return null;
    }

    function reportColor(force) {
        var newColor = extractColor();
        if (!newColor || !window._ColorThemeBridge) return;
        if (!force && newColor === _wtc._lastColor) return;
        // 已有品牌色/导航色时，不要被后续延迟扫描的白色 body 采样覆盖（搜狐黄→白闪烁）
        if (isNearWhite(newColor) && _wtc._lastColor &&
            !isNearWhite(_wtc._lastColor) && !isNearBlack(_wtc._lastColor)) {
            return;
        }
        _wtc._lastColor = newColor;
        window._ColorThemeBridge.onColorExtracted(newColor);
    }

    reportColor(true);

    function observeThemeMeta() {
        if (_wtc._metaObserver) return;
        var meta = document.querySelector('meta[name="theme-color"]');
        if (!meta) return;
        _wtc._metaObserver = new MutationObserver(function() {
            reportColor(false);
        });
        _wtc._metaObserver.observe(meta, {
            attributes: true,
            attributeFilter: ['content', 'value']
        });
    }

    observeThemeMeta();

    function isTopBarMutation(mutations) {
        for (var i = 0; i < mutations.length; i++) {
            var m = mutations[i];
            var target = m.target;
            if (!target || target.nodeType !== 1) continue;
            if (target.tagName === 'HEAD' || (target.closest && target.closest('head'))) continue;
            if (m.type === 'attributes') {
                var rect = target.getBoundingClientRect();
                if (rect.top <= 88 && rect.bottom > 0 && rect.height >= 8) return true;
                var tag = (target.tagName || '').toLowerCase();
                var cls = (target.className && target.className.toString().toLowerCase()) || '';
                var id = (target.id && target.id.toLowerCase()) || '';
                if (tag === 'header' || tag === 'nav' || target.getAttribute('role') === 'banner') return true;
                if (/header|navbar|nav-bar|topbar|top-bar|app-bar|toolbar/.test(cls + ' ' + id)) return true;
            }
            if (m.type === 'childList' && m.addedNodes) {
                for (var j = 0; j < m.addedNodes.length; j++) {
                    var node = m.addedNodes[j];
                    if (!node || node.nodeType !== 1) continue;
                    var r = node.getBoundingClientRect();
                    if (r.top <= 88 && r.height >= 8) return true;
                }
            }
        }
        return false;
    }

    // 恢复顶栏 class/style 监听（搜狐等站点 hydration 后需重取色）；
    // 滚动期间跳过，避免百度顶栏 class 切换误触发。
    if (!_wtc._scrollGuardBound) {
        _wtc._scrollGuardBound = true;
        _wtc._scrollActive = false;
        window.addEventListener('scroll', function() {
            _wtc._scrollActive = true;
            clearTimeout(_wtc._scrollIdleTimer);
            _wtc._scrollIdleTimer = setTimeout(function() { _wtc._scrollActive = false; }, 300);
        }, { passive: true });
    }

    if (!_wtc._observer) {
        _wtc._observer = new MutationObserver(function(mutations) {
            if (_wtc._scrollActive) return;
            if (!isTopBarMutation(mutations)) return;
            clearTimeout(_wtc._domDebounceTimer);
            _wtc._domDebounceTimer = setTimeout(function() { reportColor(false); }, 120);
        });
        _wtc._observer.observe(document.documentElement, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['style', 'class']
        });
    }

    if (!_wtc._headObserver) {
        _wtc._headObserver = new MutationObserver(function() {
            observeThemeMeta();
        });
        var head = document.head || document.documentElement;
        _wtc._headObserver.observe(head, {
            childList: true,
            subtree: true
        });
    }

    _wtc._extracting = false;
})();
""".trimIndent()

        /** 仅读取 meta theme-color；若为近黑色则跳过，避免百度等站点首屏误报。 */
        val COLOR_EXTRACT_META_JS = """
(function() {
    try {
        if (!window._ColorThemeBridge) return;
        var meta = document.querySelector('meta[name="theme-color"]');
        if (!meta) return;
        var content = (meta.getAttribute('content') || meta.getAttribute('value') || '').trim();
        if (!content) return;
        var hex = content.startsWith('#') ? content : content;
        if (/^#?[0-9a-fA-F]{3,8}$/.test(content.replace('#',''))) {
            var h = content.replace('#','');
            if (h.length === 3) h = h.split('').map(function(c){return c+c;}).join('');
            if (h.length >= 6) {
                var r = parseInt(h.substring(0,2),16), g = parseInt(h.substring(2,4),16), b = parseInt(h.substring(4,6),16);
                if (r < 24 && g < 24 && b < 24) return;
                var hex = '#' + h.substring(0, 6);
                var _wtc = window._wtc || {};
                window._wtc = _wtc;
                _wtc._lastColor = hex;
                window._ColorThemeBridge.onColorExtracted(hex);
                return;
            }
        }
        window._ColorThemeBridge.onColorExtracted(content);
    } catch (e) {}
})();
""".trimIndent()

        fun scheduleColorExtractMeta(webView: WebView) {
            webView.evaluateJavascript(COLOR_EXTRACT_META_JS, null)
        }

        /** 立即取 meta + 全量扫描，并在首帧后短间隔重试（接近 Chrome 的 commit-visible 体验）。 */
        fun scheduleColorExtract(webView: WebView) {
            webView.evaluateJavascript(COLOR_EXTRACT_META_JS, null)
            webView.evaluateJavascript(COLOR_EXTRACT_JS, null)
            webView.postDelayed({ webView.evaluateJavascript(COLOR_EXTRACT_META_JS, null) }, 16)
            webView.postDelayed({ webView.evaluateJavascript(COLOR_EXTRACT_JS, null) }, 80)
            webView.postDelayed({ webView.evaluateJavascript(COLOR_EXTRACT_JS, null) }, 200)
        }
    }

    private var lastColor: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onColorExtracted(color: String) {
        val trimmed = color.trim()
        if (trimmed.isBlank()) return

        mainHandler.post {
            val normalized = try {
                val parsed = android.graphics.Color.parseColor(
                    if (trimmed.startsWith("#")) trimmed else when {
                        trimmed.startsWith("rgb") -> trimmed
                        else -> "#$trimmed"
                    }
                )
                String.format("#%06X", 0xFFFFFF and parsed)
            } catch (_: Exception) {
                trimmed
            }
            if (normalized == lastColor) return@post
            lastColor = normalized
            AppLogger.d("ColorThemeBridge", "网页颜色提取: $normalized")
            onColorChanged(normalized)
        }
    }

    @JavascriptInterface
    fun removeColorTheme() {
        AppLogger.d("ColorThemeBridge", "清除网页颜色主题")
        onColorCleared()
    }

    /**
     * 页面切换时重置去重，便于同色页面也能在导航后重新上报。
     */
    fun onPageChanged(url: String?) {
        lastColor = null
    }
}
