package cn.xeblog.plugin.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author eleven
 * @date 2023/12/12 10:27
 * @apiNote
 */
@Data
public class Mask {
    private List<String> maskUsernames;

    private List<String> maskIps;

    private List<String> maskRegions;

    private Boolean notShow;

    public void clear() {
        maskIps.clear();
        maskRegions.clear();
        maskUsernames.clear();
    }

    public Mask() {
        this.maskUsernames = new ArrayList<>();
        this.maskIps = new ArrayList<>();
        this.maskRegions = new ArrayList<>();
    }

}
