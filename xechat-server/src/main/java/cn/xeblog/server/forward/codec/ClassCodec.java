package cn.xeblog.server.forward.codec;

import com.google.gson.*;
import java.lang.reflect.Type;

/**
 * 协议共享类，与 xechat-forward-match-lobby 中同名类保持同步。
 *
 * @author eleven
 * @date 2024/12/4 9:22
 */
public class ClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

    @Override
    public Class deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String className = json.getAsString();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public JsonElement serialize(Class src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getName());
    }
}
