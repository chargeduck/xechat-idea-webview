package cn.xeblog.commons.entity.game.zillionaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.awt.*;

/**
 * @author eleven
 * @date 2023/3/20 13:30
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PositionDto{

    /**
     * 位置
     */
    private Integer position;

    /**
     * 是城市
     */
    private Boolean isCity;

    private Boolean allowBuy;

    /**
     * 名字
     */
    private String name;

    /**
     * 颜色
     */
    private Color color;

    /**
     * 拥有者
     */
    private String owner;

    /**
     * 是否允许升级
     */
    private Boolean upgradeAllowed;

    /**
     * 行动
     */
    private Integer action;

    /**
     * 状态 默认 true， false代表待赎回
     */
    private Boolean positionStatus;

    public PositionDto(Integer position, String name, Integer action) {
        this.position = position;
        this.name = name;
        this.owner = null;
        this.action = action;
        this.isCity = false;
        this.upgradeAllowed = false;
        this.positionStatus = true;
        this.allowBuy = false;
        this.color = Color.BLACK;
        this.positionStatus = true;
    }
}
