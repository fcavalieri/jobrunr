package org.jobrunr.dashboard;

import org.jobrunr.utils.FreePortFinder;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

public class JsonbJobRunrDashboardWebserverTest extends JobRunrDashboardWebServerTest {

    @Override
    public JsonMapper getJsonMapper() {
        return new JsonbJsonMapper();
    }

    @Override
    public JobRunrDashboardWebServerConfiguration getDashboardConfiguration() {
        int portHttp = FreePortFinder.nextFreePort(8000);
        return JobRunrDashboardWebServerConfiguration
                .usingStandardDashboardConfiguration()
                .andPort(portHttp)
                .andEnableHttp(true);
    }
}
