package org.jobrunr.dashboard;

import org.jobrunr.utils.FreePortFinder;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

public class HttpsSSJobRunrDashboardWebserverTest extends JobRunrDashboardWebServerTest {

    @Override
    public JsonMapper getJsonMapper() {
        return new JacksonJsonMapper();
    }

    @Override
    public JobRunrDashboardWebServerConfiguration getDashboardConfiguration() {
        int portHttp = FreePortFinder.nextFreePort(8000);
        int portHttps = FreePortFinder.nextFreePort(portHttp + 1);
        return JobRunrDashboardWebServerConfiguration
                .usingStandardDashboardConfiguration()
                .andPort(portHttp)
                .andPortHttps(portHttps)
                .andEnableHttp(true)
                .andEnableHttps(true);
    }
}
