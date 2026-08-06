package cn.xeblog.plugin.mode;

import cn.xeblog.plugin.enums.Style.StyleDTO;

/**
 * 消息渲染模式接口（Vue 迁移版）。
 * handleStyle 去掉 MutableAttributeSet，改为接收和返回 StyleDTO。
 *
 * @author anlingyi
 * @date 2020/9/1
 */
public interface Mode {

    default void init() {}

    /**
     * 处理样式 DTO，可修改 color / bold 以实现主题模式
     */
    default StyleDTO handleStyle(StyleDTO style) {
        return style;
    }

    default void renderTextBefore(String text) {
    }
}
