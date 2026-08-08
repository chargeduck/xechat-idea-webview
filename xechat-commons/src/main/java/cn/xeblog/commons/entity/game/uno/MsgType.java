package cn.xeblog.commons.entity.game.uno;

public enum MsgType {
    /**
     * 刷新提示味精
     */
    REFRESH_TIPS_MSG,
    /**
     * 加入机器人
     */
    JOIN_ROBOTS,
    /**
     * 出牌
     */
    OUT_CARDS,
    /**
     * uno
     */
    UNO,
    /**
     * 抓
     */
    CATCH,
    /**
     * 质疑
     */
    QUESTION,
    /**
     * 分牌
     */
    ALLOC_CARDS,
    /**
     * 初始化分牌
     */
    INIT_ALLOC_CARDS,
    /**
     * init丢弃
     */
    INIT_DISCARD,
    /**
     * 改变颜色
     */
    CHANGE_COLOR,
    /**
     * 通过
     */
    PASS
    ;
}
