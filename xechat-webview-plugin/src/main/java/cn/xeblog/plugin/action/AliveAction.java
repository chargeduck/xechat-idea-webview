package cn.xeblog.plugin.action;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * 存活检测操作类（Vue 迁移版）。
 * 保留定时器/倒计时逻辑，移除所有 Swing UI（AliveDialogWrapper）。
 * 倒计时回调通过 Consumer 接口传递给前端 JSBridge。
 *
 * @author anlingyi
 * @date 2021/9/4 12:39 下午
 */
public class AliveAction {

    /**
     * 是否正在运行
     */
    private static boolean running;

    /**
     * 是否开启该功能
     */
    private static boolean enabled;

    /**
     * 工作时间，单位：秒
     */
    private static int workTime = 1 * 60 * 60;

    /**
     * 休息时间，单位：秒
     */
    private static int restTime = 10 * 60;

    /**
     * 下一次提醒的时间（精确到秒的时间戳）
     */
    private static long nextStartTime;

    /**
     * 倒计时回调：currentSecond -> 剩余秒数，返回 true 表示用户确认（点击"肝"），false 表示倒计时结束
     */
    private static Consumer<Integer> countdownCallback;

    /**
     * 弹窗关闭回调：true 表示用户主动退出休息
     */
    private static Consumer<Boolean> dismissCallback;

    /**
     * 倒计时文本回调：hh:mm:ss 格式的倒计时字符串，供前端展示
     */
    private static Consumer<String> tickCallback;

    private static final String GAN = "我只想搞钱";

    public static void setWorkTime(int second) {
        if (second < 0) {
            return;
        }
        workTime = second;
    }

    public static int getWorkTime() {
        return workTime;
    }

    public static void setRestTime(int second) {
        if (second < 0) {
            return;
        }
        restTime = second;
    }

    public static int getRestTime() {
        return restTime;
    }

    public static void setEnabled(boolean bool) {
        if (bool) {
            if (!enabled) {
                setNextStartTime();
                run();
            }
        } else {
            running = false;
        }
        enabled = bool;
    }

    public static void setCountdownCallback(Consumer<Integer> callback) {
        countdownCallback = callback;
    }

    public static void setDismissCallback(Consumer<Boolean> callback) {
        dismissCallback = callback;
    }

    public static void setTickCallback(Consumer<String> callback) {
        tickCallback = callback;
    }

    /**
     * 用户在前端确认输入"我只想搞钱"后由 JSBridge 调用
     */
    public static void confirmAlive(String inputText) {
        if (GAN.equals(inputText)) {
            stop(true);
        }
    }

    /**
     * 用户点击关闭按钮后由 JSBridge 调用
     */
    public static void dismissAlive() {
        stop(true);
    }

    private static long getNowTimeSecond() {
        return System.currentTimeMillis() / 1000;
    }

    public static boolean flushNextStartTime() {
        if (getNowTimeSecond() > nextStartTime) {
            setNextStartTime();
            return true;
        }
        return false;
    }

    private static void setNextStartTime() {
        nextStartTime = getNowTimeSecond() + workTime;
    }

    public static long getNextStartTime() {
        return nextStartTime;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isContinued() {
        return enabled && running;
    }

    private static void run() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!enabled) {
                    timer.cancel();
                    return;
                }
                if (getNowTimeSecond() == nextStartTime) {
                    areYouOk();
                }
            }
        }, 0, 1000);
    }

    /**
     * 触发存活检测弹窗。
     * 通过回调通知前端显示 Vue 弹窗并启动倒计时。
     */
    private static void areYouOk() {
        if (running) {
            return;
        }
        running = true;

        // TODO: JSBridge 通知前端显示存活检测弹窗
        // 前端应调用 countdownCallback、tickCallback、dismissCallback
        // 倒计时逻辑由前端实现，Java 端不再管理倒计时 UI

        if (countdownCallback != null) {
            countdownCallback.accept(restTime);
        }

        startCountdown(restTime);
    }

    /**
     * 服务端倒计时（用于超时自动关闭）。
     * 前端展示独立的倒计时 UI，此方法确保服务端状态正确。
     */
    private static void startCountdown(int totalSeconds) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            int remaining = totalSeconds;

            @Override
            public void run() {
                if (!running || --remaining < 0) {
                    timer.cancel();
                    stop(false);
                    return;
                }

                String timeStr = formatTime(remaining);
                if (tickCallback != null) {
                    tickCallback.accept(timeStr);
                }
            }
        }, 0, 1000);
    }

    /**
     * 格式化秒数为 hh:mm:ss
     */
    public static String formatTime(int totalSeconds) {
        int hour = totalSeconds / 3600;
        int minute = (totalSeconds % 3600) / 60;
        int second = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    private static void stop(boolean exit) {
        running = false;
        if (exit) {
            setNextStartTime();
        }
        if (dismissCallback != null) {
            dismissCallback.accept(exit);
        }
    }
}
