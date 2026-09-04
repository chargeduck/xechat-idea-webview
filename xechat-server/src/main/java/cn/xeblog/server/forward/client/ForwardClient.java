package cn.xeblog.server.forward.client;

import cn.xeblog.server.cache.ForwardCache;
import cn.xeblog.server.config.ServerConfig;
import cn.xeblog.server.forward.utils.MessageBuilder;
import cn.xeblog.server.forward.utils.XeServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author eleven
 * @date 2024/12/16 9:51
 * @apiNote 连入 hub(lobby) 的转发客户端。
 * <p>
 * 相对老 xe-forwarding-server 的增强：
 * 1. 连接失败/中断后 channel() 会检测连接状态并自动重建，避免拿到已关闭的 channel 持续写失败；
 * 2. 未配置 forwardHost 时直接返回 null（不启用转发），调用方需判空；
 * 3. 注册最多重试 {@link #MAX_ATTEMPTS} 次，失败后停止自动重试（givenUp），防止反复同步建连阻塞主进程；
 *    运行中可通过 forward 管理员命令 resetAndRetry() 重置计数并重新注册。
 */
@Slf4j
public class ForwardClient {

    /**
     * 注册重试上限（次）
     */
    public static final int MAX_ATTEMPTS = 3;

    /**
     * 单次建连超时（毫秒），避免同步 connect 长时间卡住调用线程
     */
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;

    private static volatile Channel channel;

    private static volatile int failedAttempts;

    private static volatile boolean givenUp;

    private static final Object LOCK = new Object();

    public static Channel channel() {
        if (!enabled()) {
            return null;
        }
        if (channel != null && channel.isActive()) {
            return channel;
        }
        // 已连续失败达到上限，不再自动重试，避免反复同步建连阻塞主进程
        if (givenUp) {
            return null;
        }
        /**
         * 防止有两个线程 t1 t2同时进来时 channel 为空。
         * t1 使用 initChannel 创建线程之后，t2进来再次初始化 channel
         * 也称之为双检锁, 在 1.5之前可能因为指令重排序出现问题，
         * 使用 volatile 防止指令重排序, 保证双检锁的正确性
         */
        synchronized (LOCK) {
            if (givenUp) {
                return null;
            }
            if (channel != null && channel.isActive()) {
                return channel;
            }
            initChannel();
            return channel;
        }
    }

    /**
     * 重新连接（channel 存在但已断开时重建；已放弃重试时不再触发）
     */
    public static void reconnect() {
        if (givenUp) {
            return;
        }
        if (channel != null && !channel.isActive()) {
            synchronized (LOCK) {
                if (givenUp) {
                    return;
                }
                if (channel != null && !channel.isActive()) {
                    initChannel();
                }
            }
        }
    }

    /**
     * 是否已放弃自动重试（连续 {@link #MAX_ATTEMPTS} 次注册失败）
     */
    public static boolean hasGivenUp() {
        return givenUp;
    }

    /**
     * 是否启用转发（forwardHost 已配置）
     */
    public static boolean enabled() {
        ServerConfig config = ServerConfig.getConfig();
        return config != null && config.getForwardHost() != null && !config.getForwardHost().isEmpty();
    }

    /**
     * 重置失败计数并立即重新注册（forward 管理员命令使用），返回是否注册成功
     */
    public static boolean resetAndRetry() {
        if (!enabled()) {
            return false;
        }
        synchronized (LOCK) {
            givenUp = false;
            failedAttempts = 0;
            Channel old = channel;
            channel = null;
            if (old != null) {
                try {
                    old.close();
                } catch (Exception ignore) {
                    // 关闭旧连接失败不影响重新注册
                }
            }
            initChannel();
            return channel != null && channel.isActive();
        }
    }

    /**
     * 服务器接入名发生变化（探测完成）时补发一次心跳，让 hub 及时更新鱼塘名单展示
     */
    public static void notifyServerInfoChanged() {
        Channel ch = channel;
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(MessageBuilder.heartbeat(ForwardCache.channelShortId, ForwardCache.seq,
                    ForwardCache.serverName, XeServerUtils.getServerInfoJsonStr()));
        }
    }

    /**
     * 初始化channel
     */
    private static void initChannel() {
        ServerConfig config = ServerConfig.getConfig();
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                .handler(new ForwardClientInitializer());
        try {
            channel = bootstrap.connect(config.getForwardHost(), config.getForwardPort()).sync().channel();
            // 关闭channel不能使用sync让客户端阻塞，所以使用addListener
            channel.closeFuture().addListener(future -> group.shutdownGracefully());
            failedAttempts = 0;
            givenUp = false;
            log.info("ForwardClient 初始化成功, host -> {}, port -> {}", config.getForwardHost(), config.getForwardPort());
        } catch (Exception e) {
            channel = null;
            group.shutdownGracefully();
            failedAttempts++;
            if (failedAttempts >= MAX_ATTEMPTS) {
                givenUp = true;
                log.warn("ForwardClient 连续 {} 次注册失败，停止自动重试（可执行 forward 命令重新注册）", MAX_ATTEMPTS);
            } else {
                log.warn("ForwardClient 连接失败({}/{}): {}", failedAttempts, MAX_ATTEMPTS, e.getMessage());
            }
        }
    }
}
