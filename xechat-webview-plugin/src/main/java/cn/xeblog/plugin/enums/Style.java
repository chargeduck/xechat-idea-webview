package cn.xeblog.plugin.enums;

import cn.xeblog.plugin.mode.ModeContext;

/**
 * 文本样式枚举（Vue 迁移版）。
 * 去掉 Swing MutableAttributeSet/StyleConstants/JBColor/Color，
 * 改为返回纯数据 StyleDTO（含 color、bold 字段）。
 *
 * @author anlingyi
 * @date 2020/8/21
 */
public enum Style {

    DEFAULT("default", false),
    USER_NAME("default", true),
    SYSTEM_MSG("#E85158", true),
    WARN("#E85158", false),
    LIGHT("#F18787", false),
    BOLD("default", true);

    private final StyleDTO style;

    Style(String color, boolean bold) {
        this.style = new StyleDTO(color, bold);
    }

    /**
     * 获取样式 DTO（经 Mode 处理后）
     */
    public StyleDTO get() {
        return ModeContext.getMode().handleStyle(style);
    }

    /**
     * 获取原始样式 DTO（忽略 Mode 处理）
     */
    public StyleDTO getByIgnoreMode() {
        return style;
    }

    public static void initStyle() {
        // 样式已在枚举初始化时构建，无需额外初始化
    }

    /**
     * 纯数据样式 DTO
     * @param color 颜色值：十六进制字符串（如 "#E85158"）或 "default"（使用主题默认色）
     * @param bold  是否加粗
     */
    public record StyleDTO(String color, boolean bold) {
    }
}
