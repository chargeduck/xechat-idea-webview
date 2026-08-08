package cn.xeblog.commons.entity.game.uno;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;

/**
 * @author eleven
 * @date 2023/4/8 22:20
 * @apiNote 红黄蓝绿
 * 0 一张
 * 1 2 3 4 5 6 7 8 9 禁止 reverse +2 两张
 * 黑色
 * 换颜色  +4 各四张
 * 欢乐模式
 * CLEAR 每种颜色两张
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card implements Comparable<Card> {

    private Integer id;

    /**
     * 评分
     */
    private Integer score;

    /**
     * 值
     */
    private String value;

    /**
     * 颜色
     */
    private Color color;

    /**
     * 是否功能牌
     */
    private Boolean isFunctionCard;

    private Color changeColor;

    @Override
    public int compareTo(Card card) {
        if (!color.equals(card.getColor())) {
            return colorSort(color).compareTo(colorSort(card.getColor()));
        }
        return card.getScore().compareTo(score);
    }

    private Integer colorSort(Color color) {
        if (color.equals(Color.RED)) {
            return 1;
        }
        if (color.equals(Color.YELLOW)) {
            return 2;
        }
        if (color.equals(Color.GREEN)) {
            return 3;
        }
        if (color.equals(Color.BLUE)) {
            return 4;
        }

        return 0;
    }

    public String getToolTipText() {
        switch (getValue().toUpperCase()) {
            case "CHANGE":
                return "CHANGE 改变下次出牌的颜色";
            case "+2":
                return "使下家摸两张牌并跳过一回合";
            case "+4":
                return "改变下次出牌颜色并使下家摸四张牌跳过当前回合，如果下家质疑成功则你摸6张";
            case "REVERSE":
                return "翻转出牌顺序";
            case "CLEAR":
                return "清空所有同颜色的牌";
            case "SKIP" :
                return "下家跳过";
            default:
                return getColorStr() + " - " + value;
        }
    }

    public String getColorStr() {
        if (color.equals(Color.RED)) {
            return "红色";
        } else if (color.equals(Color.YELLOW)) {
            return "黄色";
        } else if (color.equals(Color.GREEN)) {
            return "绿色";
        } else if (color.equals(Color.BLUE)) {
            return "蓝色";
        }
        return "黑色";
    }

    @Override
    public String toString() {
        return "Card{" +
                "score=" + score +
                ", value='" + value + '\'' +
                ", color=" + getColorStr() +
                ", isFunctionCard=" + isFunctionCard +
                '}';
    }

    public Card(Integer score, String value, Color color, Boolean isFunctionCard) {
        this.score = score;
        this.value = value;
        this.color = color;
        this.isFunctionCard = isFunctionCard;
    }
}
