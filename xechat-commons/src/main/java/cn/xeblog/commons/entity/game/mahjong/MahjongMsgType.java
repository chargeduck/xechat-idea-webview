package cn.xeblog.commons.entity.game.mahjong;

/**
 * @author eleven
 * @date 2024/3/24 15:27
 * @apiNote
 */
public enum MahjongMsgType {
    /**
     * 加入机器人
     */
    JOIN_ROBOTS,
    /**
     * 分牌
     */
    ALLOC_MAHJONG,
    /**
     * 指定庄家
     */
    IDENTIFY_BANKER,
    /**
     * 出牌
     */
    OUT_MAHJONG,
    /**
     * 杠
     */
    GANG,
    /**
     * 暗杠
     */
    AN_GANG,
    /**
     * 碰
     */
    PENG,
    /**
     * 吃
     */
    CHI,
    /**
     * 胡
     */
    HU,
    /**
     * 从头摸
     */
    HEAD_TOUCH,
    /**
     * 从尾部摸排
     */
    TAIL_TOUCH
    ;
}
