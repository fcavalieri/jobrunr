package org.jobrunr.tests.e2e;

import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;

public abstract class AbstractE2EGsonTest extends AbstractE2ETest {

    @Test
    void onlyGsonIsOnClasspath() {
        assertThat(classExists("com.google.gson.Gson")).isTrue();
        assertThat(classExists("com.fasterxml.jackson.databind.ObjectMapper")).isFalse();
        assertThat(storageProvider.getJobMapper().getJsonMapper() instanceof GsonJsonMapper).isTrue();
    }

}
