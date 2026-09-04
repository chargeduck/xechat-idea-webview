package cn.xeblog.server.forward.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 协议共享接口，与 xechat-forward-match-lobby 中同名接口保持同步。
 *
 * @author eleven
 * @date 2024/12/16 8:13
 */
public interface MessageSerializer {

    <T> T deserialize(Class<T> clazz, byte[] bytes);

    <T> byte[] serialize(T object);

    enum Algorithm implements MessageSerializer {
        JAVA {
            @Override
            public <T> T deserialize(Class<T> clazz, byte[] bytes) {
                try {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                    return (T) ois.readObject();
                } catch (Exception e) {
                    throw new RuntimeException("反序列化失败", e);
                }
            }

            @Override
            public <T> byte[] serialize(T object) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(object);
                    return baos.toByteArray();
                } catch (Exception e) {
                    throw new RuntimeException("序列化失败", e);
                }
            }
        },
        JSON {
            private final Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Class.class, new ClassCodec())
                    .create();

            @Override
            public <T> T deserialize(Class<T> clazz, byte[] bytes) {
                return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), clazz);
            }

            @Override
            public <T> byte[] serialize(T object) {
                return gson.toJson(object).getBytes(StandardCharsets.UTF_8);
            }
        }
    }
}
