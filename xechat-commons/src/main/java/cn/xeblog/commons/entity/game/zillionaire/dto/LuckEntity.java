package cn.xeblog.commons.entity.game.zillionaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author eleven
 * @date 2023/3/27 14:29
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LuckEntity {
    /**
     * id
     */
    private Integer id;

    /**
     * 名称
     */
    private String title;

    /**
     * 动作
     */
    private String action;

    /**
     * 描述
     */
    private String description;

    private Integer type;
}
