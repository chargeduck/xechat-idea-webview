package cn.xeblog.plugin.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.xeblog.commons.entity.IpRegion;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @author eleven
 * @date 2024/11/7 10:58
 * @apiNote
 */
@Slf4j
public class IpRegionUtil {
    private final static String IP_QUERY_URL = "https://www.ip38.com";


    public static IpRegion ipRegion() {

        IpRegion ipRegion = new IpRegion();
        try {
            String returnHtmlStr = HttpUtil.get(IP_QUERY_URL);
            Document document = Jsoup.parse(returnHtmlStr);
            Element myIpLink = document.getElementById("myIpLink");
            String href = myIpLink.attr("href");
            String finalUrl = StrUtil.format("{}{}", IP_QUERY_URL, href);
            returnHtmlStr = HttpUtil.get(finalUrl);
            document = Jsoup.parse(returnHtmlStr);
            String mark = document.getElementsByTag("mark").get(0).text();
            String region = document.getElementsByClass("region").get(0).text();
            concatIpRegion(region, mark, ipRegion);
        } catch (Exception e) {
            NotifyUtils.error("获取ip区域信息失败", e.getMessage(), true);
            concatIpRegion("中国 山东 青岛 电信", "127.0.0.1", ipRegion);
        }
        return ipRegion;
    }

    private static void concatIpRegion(String region, String mark, IpRegion ipRegion) {
        String[] split = region.split(" ");
        ipRegion.setIp(mark);
        ipRegion.setCountry(getOrElse(split, 0, "中国"));
        ipRegion.setProvince(getOrElse(split, 1, "未知"));
        ipRegion.setCity(getOrElse(split, 2, "未知"));
        ipRegion.setIsp(getOrElse(split, 3, "未知"));
    }

    private static String getOrElse(String[] regionSplit, int index, String defaultVal) {
        try {
            return regionSplit[index];
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
