package org.jobrunr.dashboard;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import org.jobrunr.dashboard.server.TeenyHttpHandler;
import org.jobrunr.dashboard.server.TeenyWebServer;
import org.jobrunr.dashboard.server.http.RedirectHttpHandler;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.ThreadSafeStorageProvider;
import org.jobrunr.utils.annotations.VisibleFor;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.file.Path;

import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

/**
 * Provides a dashboard which gives insights in your jobs and servers.
 * The dashboard server starts by default on port 8000.
 *
 * @author Ronald Dehuysser
 */
public class JobRunrDashboardWebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrDashboardWebServer.class);

    private final StorageProvider storageProvider;
    private final JsonMapper jsonMapper;

    private final BasicAuthenticator basicAuthenticator;

    private final boolean enableHttp;
    private final int portHttp;

    private final boolean enableHttps;
    private final int portHttps;
    private final Path keyStorePathHttps;
    private final String keyStorePasswordHttps;

    private TeenyWebServer teenyWebServerHttp;
    private TeenyWebServer teenyWebServerHttps;

    public static void main(String[] args) {
        new JobRunrDashboardWebServer(null, new JacksonJsonMapper());
    }

    public JobRunrDashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this(storageProvider, jsonMapper, 8000);
    }

    public JobRunrDashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper, int portHttp) {
        this(storageProvider, jsonMapper, usingStandardDashboardConfiguration().andPort(portHttp));
    }

    public JobRunrDashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper, int portHttp, String username, String password) {
        this(storageProvider, jsonMapper, usingStandardDashboardConfiguration().andPort(portHttp).andBasicAuthentication(username, password));
    }

    public JobRunrDashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobRunrDashboardWebServerConfiguration configuration) {
        this.storageProvider = new ThreadSafeStorageProvider(storageProvider);
        this.jsonMapper = jsonMapper;

        this.enableHttp = configuration.enableHttp;
        this.portHttp = configuration.port;

        this.basicAuthenticator = createOptionalBasicAuthenticator(configuration.username, configuration.password);

        this.enableHttps = configuration.enableHttps;
        this.portHttps = configuration.portHttps;
        this.keyStorePathHttps = configuration.keyStorePathHttps;
        this.keyStorePasswordHttps = configuration.keyStorePasswordHttps;
    }

    public void start() {
        if (enableHttp) {
            teenyWebServerHttp = new TeenyWebServer(portHttp);
            initWebServer(teenyWebServerHttp);
        }
        if (enableHttps) {
            teenyWebServerHttps = new TeenyWebServer(portHttps, keyStorePathHttps, keyStorePasswordHttps);
            initWebServer(teenyWebServerHttps);
        }
    }

    private void initWebServer(TeenyWebServer teenyWebServer) {
        RedirectHttpHandler redirectHttpHandler = new RedirectHttpHandler("/", "/dashboard");
        JobRunrStaticFileHandler staticFileHandler = createStaticFileHandler();
        JobRunrApiHandler dashboardHandler = createApiHandler(storageProvider, jsonMapper);
        JobRunrSseHandler sseHandler = createSSeHandler(storageProvider, jsonMapper);

        registerContext(teenyWebServer, redirectHttpHandler);
        registerSecuredContext(teenyWebServer, staticFileHandler);
        registerSecuredContext(teenyWebServer, dashboardHandler);
        registerSecuredContext(teenyWebServer, sseHandler);

        teenyWebServer.start();
        LOGGER.info("JobRunr Dashboard started at {}://{}:{}",
                teenyWebServer.getWebServerProtocol(),
                teenyWebServer.getWebServerHostAddress(),
                teenyWebServer.getWebServerHostPort());
    }

    public void stop() {
        if (teenyWebServerHttp != null) {
            teenyWebServerHttp.stop();
            LOGGER.info("JobRunr HTTP dashboard stopped");
            teenyWebServerHttp = null;
        }
        if (teenyWebServerHttps != null) {
            teenyWebServerHttps.stop();
            LOGGER.info("JobRunr HTTPS dashboard stopped");
            teenyWebServerHttps = null;
        }
    }

    HttpContext registerContext(TeenyWebServer teenyWebServer, TeenyHttpHandler httpHandler) {
        return teenyWebServer.createContext(httpHandler);
    }

    HttpContext registerSecuredContext(TeenyWebServer teenyWebServer, TeenyHttpHandler httpHandler) {
        HttpContext httpContext = registerContext(teenyWebServer, httpHandler);
        if (basicAuthenticator != null) {
            httpContext.setAuthenticator(basicAuthenticator);
        }
        return httpContext;
    }

    @VisibleFor("github issue 18")
    JobRunrStaticFileHandler createStaticFileHandler() {
        return new JobRunrStaticFileHandler();
    }

    @VisibleFor("github issue 18")
    JobRunrApiHandler createApiHandler(StorageProvider storageProvider, JsonMapper jsonMapper) {
        return new JobRunrApiHandler(storageProvider, jsonMapper);
    }

    @VisibleFor("github issue 18")
    JobRunrSseHandler createSSeHandler(StorageProvider storageProvider, JsonMapper jsonMapper) {
        return new JobRunrSseHandler(storageProvider, jsonMapper);
    }

    private BasicAuthenticator createOptionalBasicAuthenticator(String username, String password) {
        if (isNotNullOrEmpty(username) && isNotNullOrEmpty(password)) {
            return new BasicAuthenticator("JobRunr") {
                @Override
                public boolean checkCredentials(String user, String pwd) {
                    return user.equals(username) && pwd.equals(password);
                }
            };
        }
        return null;
    }
}
