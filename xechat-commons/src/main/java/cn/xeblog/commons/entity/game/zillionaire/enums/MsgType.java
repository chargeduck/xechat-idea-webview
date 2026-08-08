package cn.xeblog.commons.entity.game.zillionaire.enums;

/**
 * @author eleven
 * @date 2023/3/30 8:40
 * @apiNote 大富翁消息类型
 */
public enum MsgType {
    /**
     * 加入机器人
     */
    JOIN_ROBOTS,
    /**
     * 支付过路费
     */
    PAY_TOLL,
    /**
     * 投掷骰子
     */
    DICE_ROLL,
    /**
     * 买地皮
     */
    BUY_POSITION,
    /**
     * 升级建筑
     */
    UPGRADE_BUILDING,
    /**
     * 销售地皮
     */
    SALE_POSITION,
    /**
     * 支付给银行
     */
    PAY_TO_BANK,
    /**
     * 进监狱
     */
    TO_JAIL,
    /**
     * 休息
     */
    REST,
    /**
     * 重新投掷
     */
    DICE_ROLL_AGAIN,
    /**
     * 机会
     */
    CHANCE,
    /**
     * 命运
     */
    DESTINY,

    /**
     * 刷新提示
     */
    REFRESH_TIPS,
    /**
     * 税
     */
    TAX,
    /**
     * 玩家破产
     */
    BROKE_EXIT,
    /**
     * 支付给其他人
     */
    PAY_TO_OTHERS,
    /**
     * 通过
     */
    PASS,
    /**
     * 再一次结果
     */
    AGAIN_RESULT,

    /**
     * 删除临时提权玩家
     */
    REMOVE_TEMP_PLAYER,
    /**
     * 摧毁
     */
    PULL_DOWN,
    ;
}
