package cn.xeblog.plugin.enums;

import cn.hutool.core.util.StrUtil;

public enum ProvinceAbbreviation {
    BEIJING("北京", "京"),
    TIANJIN("天津", "津"),
    HEBEI("河北", "冀"),
    SHANXI("山西", "晋"),
    INNER_MONGOLIA("内蒙古自治区", "蒙"),
    LIAONING("辽宁", "辽"),
    JILIN("吉林", "吉"),
    HEILONGJIANG("黑龙江", "黑"),
    SHANGHAI("上海", "沪"),
    JIANGSU("江苏", "苏"),
    ZHEJIANG("浙江", "浙"),
    ANHUI("安徽", "皖"),
    FUJIAN("福建", "闽"),
    JIANGXI("江西", "赣"),
    SHANDONG("山东", "鲁"),
    HENAN("河南", "豫"),
    HUBEI("湖北", "鄂"),
    HUNAN("湖南", "湘"),
    GUANGDONG("广东", "粤"),
    GUANGXI("广西壮族自治区", "桂"),
    HAINAN("海南", "琼"),
    CHONGQING("重庆", "渝"),
    SICHUAN("四川", "川", "蜀"),
    GUIZHOU("贵州", "贵", "黔"),
    YUNNAN("云南", "云", "滇"),
    TIBET("西藏自治区", "藏"),
    SHAANXI("陕西", "陕", "秦"),
    GANSU("甘肃", "甘", "陇"),
    QINGHAI("青海", "青"),
    NINGXIA("宁夏回族自治区", "宁"),
    XINJIANG("新疆维吾尔自治区", "新"),
    HONGKONG("香港特别行政区", "港"),
    MACAO("澳门特别行政区", "澳"),
    TAIWAN("台湾省", "台");

    private final String provinceName;
    private final String[] abbreviations;

    ProvinceAbbreviation(String provinceName, String... abbreviations) {
        this.provinceName = provinceName;
        this.abbreviations = abbreviations;
    }

    public static String getAbbreviation(String provinceName) {
        for (ProvinceAbbreviation province : ProvinceAbbreviation.values()) {
            if (StrUtil.equals(subProvinceName(province.provinceName), subProvinceName(provinceName))) {
                if (province.abbreviations.length > 0) {
                    return province.abbreviations[0];
                }
                return "未知";
            }
        }
        return "未知";
    }

    private static String subProvinceName(String provinceName) {
        return provinceName.substring(0, 2);
    }
}