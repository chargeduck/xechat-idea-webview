package cn.xeblog.plugin.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author anlingyi
 * @date 2021/9/4 9:26 上午
 */
public class IdeaUtils {

    private static final Pattern VERSION_PATTERN = Pattern.compile("<version>(.*?)</version>");

    public static String getPluginVersion() {
        try (InputStream is = IdeaUtils.class.getResourceAsStream("/META-INF/plugin.xml")) {
            if (is == null) {
                return "???";
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            Matcher matcher = VERSION_PATTERN.matcher(sb);
            return matcher.find() ? matcher.group(1) : "???";
        } catch (Exception e) {
            return "???";
        }
    }
}
