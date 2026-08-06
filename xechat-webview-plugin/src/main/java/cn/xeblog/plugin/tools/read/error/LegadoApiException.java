package cn.xeblog.plugin.tools.read.error;

/**
 * Legado API 异常（Vue 迁移版重建）。
 * 原文件作为 Swing UI 文件被删除，现重建为纯逻辑异常类。
 *
 * @author LYF
 * @date 2022-07-14
 */
public class LegadoApiException extends Exception {

    public LegadoApiException(String message) {
        super(message);
    }

    public LegadoApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public static void throwException(String message) throws LegadoApiException {
        throw new LegadoApiException(message);
    }
}
