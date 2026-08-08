package cn.xeblog.commons.entity.game.zillionaire.dto;


import java.awt.*;

/**
 * @author eleven
 * @date 2023/3/20 13:32
 * @description 功能拍
 */
public class LuckDto extends PositionDto{

    public void superConstructor(Integer position, String name, Color color){
        super.setPosition(position);
        super.setPositionStatus(true);
        super.setIsCity(false);
        super.setAllowBuy(false);
        super.setUpgradeAllowed(false);
        super.setName(name);
        super.setColor(color);
    }
}
