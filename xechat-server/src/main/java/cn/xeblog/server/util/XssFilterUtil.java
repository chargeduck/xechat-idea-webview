package cn.xeblog.server.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XSS 过滤工具
 * <p>
 * 白名单标签/属性策略，用于消息广播前的纵深防御净化。
 * 保留 markdown 常用的基础标签与 <font color> 彩字功能，剔除脚本、事件属性与危险协议。
 *
 * @author marvis
 */
public class XssFilterUtil {

    private static final Set<String> ALLOWED_TAGS = new HashSet<>(Arrays.asList(
            "b", "i", "u", "s", "em", "strong", "span", "font", "code", "pre",
            "p", "br", "hr", "blockquote", "ul", "ol", "li",
            "table", "thead", "tbody", "tr", "td", "th",
            "h1", "h2", "h3", "h4", "h5", "h6"
    ));

    /** 允许出现在 style 属性中的 CSS 属性名 */
    private static final Set<String> ALLOWED_CSS_PROPS = new HashSet<>(Arrays.asList(
            "color", "background", "background-color", "background-image",
            "text-shadow", "font-weight", "font-style", "font-size", "font-family",
            "text-decoration", "line-height", "letter-spacing", "border",
            "border-radius", "padding", "margin", "text-align"
    ));

    private static final Pattern TAG_PATTERN = Pattern.compile("<(/)?([a-zA-Z][a-zA-Z0-9-]*)((?:\\s+[^<>]*?)?\\s*/?)>");
    private static final Pattern ATTR_PATTERN = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");

    private XssFilterUtil() {
    }

    /**
     * 净化 HTML 片段：白名单标签与属性保留，其余标签转义为实体。
     */
    public static String sanitize(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        Matcher m = TAG_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            boolean closing = m.group(1) != null;
            String tag = m.group(2).toLowerCase(Locale.ROOT);
            String attrsRaw = m.group(3) == null ? "" : m.group(3);
            String full = m.group(0);
            if (!ALLOWED_TAGS.contains(tag)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(escapeHtml(full)));
                continue;
            }
            if (closing) {
                m.appendReplacement(sb, "</" + tag + ">");
                continue;
            }
            boolean selfClose = full.trim().endsWith("/>");
            String attrs = sanitizeAttrs(tag, attrsRaw);
            m.appendReplacement(sb, "<" + tag + attrs + (selfClose ? "/>" : ">"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String sanitizeAttrs(String tag, String attrsRaw) {
        StringBuilder sb = new StringBuilder();
        Matcher am = ATTR_PATTERN.matcher(attrsRaw);
        while (am.find()) {
            String name = am.group(1).toLowerCase(Locale.ROOT);
            String value = am.group(2) != null ? am.group(2)
                    : (am.group(3) != null ? am.group(3) : (am.group(4) != null ? am.group(4) : ""));
            // 剔除事件属性
            if (name.startsWith("on")) {
                continue;
            }
            if (name.equals("style")) {
                String safe = sanitizeStyle(value);
                if (!safe.isEmpty()) {
                    sb.append(" style=\"").append(escapeAttr(safe)).append("\"");
                }
                continue;
            }
            if (name.equals("href") || name.equals("src")) {
                if (!isSafeUrl(value)) {
                    continue;
                }
                sb.append(" ").append(name).append("=\"").append(escapeAttr(value)).append("\"");
                continue;
            }
            if (!isAllowedAttr(tag, name)) {
                continue;
            }
            sb.append(" ").append(name).append("=\"").append(escapeAttr(value)).append("\"");
        }
        return sb.toString();
    }

    private static boolean isAllowedAttr(String tag, String name) {
        switch (tag) {
            case "font":
                return name.equals("color");
            case "span":
                return name.equals("style") || name.equals("title");
            case "a":
                return name.equals("href") || name.equals("title");
            case "img":
                return name.equals("src") || name.equals("alt") || name.equals("title")
                        || name.equals("width") || name.equals("height");
            case "td":
            case "th":
                return name.equals("colspan") || name.equals("rowspan") || name.equals("align");
            case "table":
                return name.equals("border") || name.equals("align");
            case "ul":
            case "ol":
                return name.equals("type") || name.equals("start");
            default:
                return name.equals("title");
        }
    }

    private static String sanitizeStyle(String style) {
        if (style == null || style.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : style.split(";")) {
            int idx = part.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String prop = part.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String val = part.substring(idx + 1).trim();
            if (!ALLOWED_CSS_PROPS.contains(prop)) {
                continue;
            }
            String vlower = val.toLowerCase(Locale.ROOT);
            if (vlower.contains("expression") || vlower.contains("javascript:") || vlower.contains("url(")
                    || vlower.contains("@import") || vlower.contains("behavior")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(prop).append(": ").append(val);
        }
        return sb.toString();
    }

    private static boolean isSafeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return true;
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        return !lower.contains("javascript:") && !lower.contains("vbscript:")
                && !lower.contains("data:") && !lower.startsWith("file:");
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttr(String text) {
        return text.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
