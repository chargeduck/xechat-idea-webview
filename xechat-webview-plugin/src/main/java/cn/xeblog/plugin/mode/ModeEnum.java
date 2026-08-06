package cn.xeblog.plugin.mode;

import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.enums.Style;
import cn.xeblog.plugin.enums.Style.StyleDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息渲染模式枚举（Vue 迁移版）。
 * Mode 接口实现改为返回 StyleDTO 而非操作 MutableAttributeSet。
 *
 * @author anlingyi
 * @date 2020/9/1
 */
@AllArgsConstructor
@Getter
public enum ModeEnum implements Mode {

    DEFAULT("默认") {
        @Override
        public void init() {
            Style.initStyle();
        }
    },

    TROUBLED_WATERS("浑水摸鱼") {
        @Override
        public StyleDTO handleStyle(StyleDTO style) {
            // 浑水摸鱼模式：文字颜色设为背景色（前端使用 "background" 表示与背景同色，实现"隐形"效果）
            return new StyleDTO("background", style.bold());
        }

        @Override
        public void renderTextBefore(String text) {
            ConsoleAction.renderText("*", Style.WARN);
        }
    };

    private String name;

    public static ModeEnum getMode(int index) {
        if (index < 0 || index >= values().length) {
            return null;
        }
        return values()[index];
    }
}
