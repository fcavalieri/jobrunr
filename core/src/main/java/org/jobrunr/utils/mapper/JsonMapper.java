package org.jobrunr.utils.mapper;

import java.io.OutputStream;

public interface JsonMapper {

    String serialize(Object object);

    void serialize(OutputStream outputStream, Object object);

    <T> T deserialize(String serializedObjectAsString, Class<T> clazz);

    String serializeRaw(Object object);

    <T> T deserializeRaw(String serializedObjectAsString, Class<T> clazz);
}
