package cn.xeblog.commons.entity.game.zillionaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;

/**
 * @author eleven
 * @date 2023/3/28 9:12
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationDto extends PositionDto{

    private Integer level;

    private Integer price;

    /**
     * 一台价格
     */
    private Integer oneStationPrice;
    /**
     * 两个站价格
     */
    private Integer twoStationPrice;
    /**
     * 三个站价格
     */
    private Integer threeStationPrice;
    /**
     * 四站价格
     */
    private Integer fourStationPrice;


    public void superConstructor(Integer position, String name){
        this.level = 0;
        this.price = 1000;
        this.oneStationPrice = 250;
        this.twoStationPrice = 500;
        this.threeStationPrice = 1000;
        this.fourStationPrice = 2000;
        super.setAllowBuy(true);
        super.setPosition(position);
        super.setName(name);
        super.setIsCity(false);
        super.setUpgradeAllowed(false);
        super.setColor(Color.BLACK);
        super.setPositionStatus(true);
    }

    public Integer getToll(){
        switch (level) {
            case 2 :
                return twoStationPrice;
            case 3:
                return threeStationPrice;
            case 4 :
                return fourStationPrice;
            default:
                return oneStationPrice;
        }
    }
}
