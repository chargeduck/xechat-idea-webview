package cn.xeblog.server.cache;

import cn.xeblog.commons.entity.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外塘用户在线视图（forward hub 聚合回灌的跨塘用户，绝不写入本塘 UserCache）
 * <p>
 * 仅用于在线列表展示与 @ 候选维度参与：短地区已带“源塘名 -> ”前缀，
 * channel 为 null（跨 JVM 反序列化，transient 字段不传输），不做任何本塘直发。
 *
 * @author eleven
 * @date 2026/09/08
 */
public final class ForwardUserCache {

    private static final Map<String, User> USER_MAP = new ConcurrentHashMap<>(32);

    private ForwardUserCache() {
    }

    /**
     * 外塘用户上线入库（key 取 uuid，uuid 缺失时回退 username）
     */
    public static void addUser(User user) {
        if (user == null) {
            return;
        }
        String key = keyOf(user);
        if (key == null) {
            return;
        }
        USER_MAP.put(key, user);
    }

    /**
     * 按 uuid 移除（USER_OFFLINE 携带 User 帧走此路径）
     */
    public static User removeByUuid(String uuid) {
        return uuid == null ? null : USER_MAP.remove(uuid);
    }

    /**
     * 按 username 移除（兼容 hub 老帧 String data），返回被移除用户（可能为 null）
     */
    public static User removeByUsername(String username) {
        if (username == null) {
            return null;
        }
        for (Map.Entry<String, User> entry : USER_MAP.entrySet()) {
            User user = entry.getValue();
            if (username.equals(user.getUsername())) {
                USER_MAP.remove(entry.getKey());
                return user;
            }
        }
        return null;
    }

    /**
     * 批量移除（SERVER_OFFLINE 携带断塘用户列表走此路径）
     */
    public static void removeAll(Collection<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        users.forEach(user -> {
            if (user == null) {
                return;
            }
            String uuid = user.getUuid();
            if (uuid != null) {
                USER_MAP.remove(uuid);
            } else {
                removeByUsername(user.getUsername());
            }
        });
    }

    /**
     * 整表替换（hub 聚合 reply：先清空再灌入外塘全量视图）
     */
    public static void resetAll(List<User> users) {
        USER_MAP.clear();
        if (users != null) {
            users.forEach(ForwardUserCache::addUser);
        }
    }

    /**
     * 外塘用户快照（含源塘名短地区前缀，供合并下发与日志）
     */
    public static List<User> listUser() {
        return new ArrayList<>(USER_MAP.values());
    }

    public static int size() {
        return USER_MAP.size();
    }

    private static String keyOf(User user) {
        String uuid = user.getUuid();
        if (uuid != null && !uuid.isEmpty()) {
            return uuid;
        }
        return user.getUsername();
    }
}
