package cn.xeblog.commons.entity.game.zillionaire.dto;

import cn.xeblog.commons.entity.game.GameDTO;
import cn.xeblog.commons.entity.game.zillionaire.enums.MsgType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author eleven
 * @date 2023/3/30 8:58
 * @apiNote
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class MonopolyGameDto extends GameDTO {
    /**
     * msg类型
     */
    private MsgType msgType;

    /**
     * 玩家名称
     */
    private String player;

    /**
     * 动作id
     */
    private Integer actionId;

    private Object data;
}
