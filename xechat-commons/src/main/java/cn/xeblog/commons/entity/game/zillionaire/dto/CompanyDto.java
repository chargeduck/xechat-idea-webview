package cn.xeblog.commons.entity.game.zillionaire.dto;

import lombok.Data;

import java.awt.*;

/**
 * @author eleven
 * @date 2023/3/28 9:24
 * @description
 */
@Data
public class CompanyDto extends PositionDto{

    private Integer price;

    public void superConstructor(Integer position, String name){
        super.setPosition(position);
        super.setPositionStatus(true);
        super.setIsCity(false);
        super.setAllowBuy(true);
        super.setUpgradeAllowed(false);
        super.setName(name);
        super.setColor(Color.BLACK);
        this.price = 1000;
    }
}
