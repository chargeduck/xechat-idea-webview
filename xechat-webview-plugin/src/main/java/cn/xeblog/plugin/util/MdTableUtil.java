package cn.xeblog.plugin.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 表格生成工具，替代 hutool ConsoleTable。
 * 产出 @{@code | header | header |} 格式的 Markdown 表格字符串。
 *
 * @author anlingyi
 */
public class MdTableUtil {

    private final List<String> headers = new ArrayList<>();
    private final List<String[]> rows = new ArrayList<>();

    public static MdTableUtil create() {
        return new MdTableUtil();
    }

    public MdTableUtil addHeader(String... headers) {
        for (var h : headers) {
            this.headers.add(h);
        }
        return this;
    }

    public MdTableUtil addBody(String... cells) {
        this.rows.add(cells);
        return this;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        int colCount = headers.size();

        // 表头行
        sb.append("| ");
        for (int i = 0; i < colCount; i++) {
            sb.append(headers.get(i)).append(" | ");
        }
        sb.append("\n");

        // 分隔线
        sb.append("|");
        for (int i = 0; i < colCount; i++) {
            sb.append("------|");
        }
        sb.append("\n");

        // 数据行
        for (var row : rows) {
            sb.append("| ");
            for (int i = 0; i < colCount; i++) {
                sb.append(row[i]).append(" | ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
