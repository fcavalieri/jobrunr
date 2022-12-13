package org.jobrunr.dashboard;

import org.jobrunr.configuration.JobRunrConfiguration;
import java.nio.file.Path;

/**
 * This class allows to configure the JobRunrDashboard
 */
public class JobRunrDashboardWebServerConfiguration {
    boolean enableHttp = true;
    int port = 8000;
    String username = null;
    String password = null;
    boolean allowAnonymousDataUsage = true;

    boolean enableHttps = false;
    int portHttps = 8001;
    Path keyStorePathHttps = null;
    String keyStorePasswordHttps = null;

    private JobRunrDashboardWebServerConfiguration() {

    }

    /**
     * This returns the default configuration with the JobRunrDashboard running on port 8000,
     * using HTTP only and no authentication
     *
     * @return the default JobRunrDashboard configuration
     */
    public static JobRunrDashboardWebServerConfiguration usingStandardDashboardConfiguration() {
        return new JobRunrDashboardWebServerConfiguration();
    }

    /**
     * Specifies whether the HTTP JobRunrDashboard will run
     *
     * @param enableHttp whether the HTTP JobRunrDashboard will run
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andEnableHttp(boolean enableHttp) {
        this.enableHttp = enableHttp;
        return this;
    }

    /**
     * Specifies the port on which the HTTP JobRunrDashboard will run
     *
     * @param port the port on which the HTTP JobRunrDashboard will run
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Adds basic authentication to the dashboard using the provided username and password.
     * <span class="strong">WARNING</span> the password will be stored in clear text and if you are using http, it can be easily intercepted.
     *
     * @param username the login which the JobRunrDashboard will ask
     * @param password the password which the JobRunrDashboard will ask
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andBasicAuthentication(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    /**
     * Allows to opt-out of anonymous usage statistics. This setting is true by default and sends only the total amount of succeeded jobs processed
     * by your cluster per day to show a counter on the JobRunr website for marketing purposes.
     *
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andAllowAnonymousDataUsage(boolean allowAnonymousDataUsage) {
        this.allowAnonymousDataUsage = allowAnonymousDataUsage;
        return this;
    }

    /**
     * Specifies whether the HTTPS JobRunrDashboard will run
     *
     * @param enableHttps whether the HTTPS JobRunrDashboard will run
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andEnableHttps(boolean enableHttps) {
        this.enableHttps = enableHttps;
        return this;
    }

    /**
     * Specifies the port on which the HTTPS JobRunrDashboard will run
     *
     * @param portHttps the port on which the HTTPS JobRunrDashboard will run
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andPortHttps(int portHttps) {
        this.portHttps = portHttps;
        return this;
    }

    /**
     * Specifies the keystore containing the certificate the JobRunrDashboard will use for HTTPS.
     *
     * @param keyStorePathHttps the keystore containing the certificate the JobRunrDashboard will use for HTTPS
     * @param keyStorePasswordHttps the keystore password (or null if no password is required)
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrDashboardWebServerConfiguration andKeyStoreHttps(Path keyStorePathHttps, String keyStorePasswordHttps) {
        this.keyStorePathHttps = keyStorePathHttps;
        this.keyStorePasswordHttps = keyStorePasswordHttps;
        return this;
    }
}
