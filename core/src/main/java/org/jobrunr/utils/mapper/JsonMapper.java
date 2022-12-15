package org.jobrunr.utils.mapper;

import java.io.OutputStream;

/**
 * JsonMapper that will transform Jobs (and their JobParameters) to Json and back for both storage in the database and
 * for use in the dashboard.
 * <p>
 * If you need to create your own JsonMapper, it must pass the {@link JsonMapperValidator#validateJsonMapper(JsonMapper)} method.
 */
public interface JsonMapper {

    String serialize(Object object);

    void serialize(OutputStream outputStream, Object object);

    <T> T deserialize(String serializedObjectAsString, Class<T> clazz);

    //JobRunrPlus: support marklogic
    String serializeRaw(Object object);

    <T> T deserializeRaw(String serializedObjectAsString, Class<T> clazz);
}
