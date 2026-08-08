package cn.xeblog.commons.entity.game.mahjong;

import cn.xeblog.commons.entity.game.GameDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author eleven
 * @date 2024/3/24 15:25
 * @apiNote
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MahjongGameDto extends GameDTO {

    private String id;

    private String prevId;

    private MahjongMsgType msgType;

    private String player;

    private Object data;

    private Boolean isRun;
}
