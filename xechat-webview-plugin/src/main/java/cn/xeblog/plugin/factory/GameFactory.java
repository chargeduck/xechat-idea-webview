package cn.xeblog.plugin.factory;

import cn.xeblog.commons.enums.Game;
import cn.xeblog.plugin.game.AbstractGame;

/**
 * WebView 架构下的游戏工厂。
 * 游戏 UI 已迁移到前端 Canvas，不再需要 @DoGame 注解的 Java 实现类。
 * produce() 返回通用桥接 AbstractGame，由 GameAction 统一管理。
 *
 * @author anlingyi
 */
public class GameFactory {

    /**
     * 生产通用游戏桥接实例。
     * 实际创建逻辑由 GameAction.create() 负责，本方法仅供兼容性保留。
     */
    public static AbstractGame produce(Game game) {
        return new AbstractGame() {
            @Override
            public void handle(cn.xeblog.commons.entity.game.GameDTO body) {
                // 数据转发由 GameAction.handle() 统一处理
            }

            @Override
            public void over() {
            }
        };
    }
}
