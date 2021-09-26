package org.jobrunr.dashboard;

import org.jobrunr.utils.FreePortFinder;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;

public class GsonJobRunrDashboardWebserverTest extends JobRunrDashboardWebServerTest {

    @Override
    public JsonMapper getJsonMapper() {
        return new GsonJsonMapper();
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
