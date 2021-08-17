package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunrConfiguration;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;

public abstract class AbstractE2EForcedJacksonTest extends AbstractE2ETest {

    @Test
    void jacksonIsUsed() {
        assertThat(classExists("com.fasterxml.jackson.databind.ObjectMapper")).isTrue();
        assertThat(classExists("com.google.gson.Gson")).isTrue();
        assertThat(storageProvider.getJobMapper().getJsonMapper() instanceof JacksonJsonMapper).isTrue();
    }

    @Override
    public JobRunrConfiguration.JsonMapperKind overrideJsonMapperKind() {
        return JobRunrConfiguration.JsonMapperKind.JACKSON;
    }
}
