package cn.xeblog.commons.entity.react.request;

import cn.xeblog.commons.entity.react.BaseReact;
import cn.xeblog.commons.entity.react.React;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author anlingyi
 * @date 2022/9/19 8:12 AM
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ReactRequest<T> extends BaseReact {

    private T body;

    private React react;

}
