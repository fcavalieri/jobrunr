package org.jobrunr.spring.autoconfigure;

import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties(prefix = "org.jobrunr")
public class JobRunrProperties {

    private JobScheduler jobScheduler = new JobScheduler();

    private Dashboard dashboard = new Dashboard();

    private BackgroundJobServer backgroundJobServer = new BackgroundJobServer();

    private Database database = new Database();

    public JobScheduler getJobScheduler() {
        return jobScheduler;
    }

    public void setJobScheduler(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public BackgroundJobServer getBackgroundJobServer() {
        return backgroundJobServer;
    }

    public void setBackgroundJobServer(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    /**
     * JobRunr JobScheduler related settings
     */
    public static class JobScheduler {

        /**
         * Enables the scheduling of jobs.
         */
        private boolean enabled = true;

        /**
         * Defines the JobDetailsGenerator to use. This should be the fully qualified classname of the
         * JobDetailsGenerator, and it should have a default no-argument constructor.
         */
        private String jobDetailsGenerator = CachingJobDetailsGenerator.class.getName();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJobDetailsGenerator() {
            return jobDetailsGenerator;
        }

        public void setJobDetailsGenerator(String jobDetailsGenerator) {
            this.jobDetailsGenerator = jobDetailsGenerator;
        }
    }

    /**
     * JobRunr BackgroundJobServer related settings
     */
    public static class BackgroundJobServer {

        /**
         * Enables the background processing of jobs.
         */
        private boolean enabled = false;

        /**
         * Sets the workerCount for the BackgroundJobServer which defines the maximum number of jobs that will be run in parallel.
         * By default, this will be determined by the amount of available processor.
         */
        private Integer workerCount;

        /**
         * Set the pollIntervalInSeconds for the BackgroundJobServer to see whether new jobs need to be processed
         */
        private Integer pollIntervalInSeconds = 15;

        /**
         * Sets the duration to wait before changing jobs that are in the SUCCEEDED state to the DELETED state. If a duration suffix
         * is not specified, hours will be used. A value of 0 disables the option. The default is 36 hours.
         */
        @DurationUnit(ChronoUnit.HOURS)
        private Duration deleteSucceededJobsAfter = Duration.ofHours(36);

        /**
         * Sets the duration to wait before changing jobs that are in the FAILED state to the DELETED state. If a duration suffix
         * is not specified, hours will be used. A value of 0 disables the option. The default is disabled.
         */
        @DurationUnit(ChronoUnit.HOURS)
        private Duration deleteFailedJobsAfter = Duration.ofHours(0);

        /**
         * Sets the duration to wait before permanently deleting jobs that are in the DELETED state. If a duration suffix
         * is not specified, hours will be used. A value of 0 disables the option. The default is 72 hours.
         */
        @DurationUnit(ChronoUnit.HOURS)
        private Duration permanentlyDeleteDeletedJobsAfter = Duration.ofHours(72);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getWorkerCount() {
            return workerCount;
        }

        public void setWorkerCount(Integer workerCount) {
            this.workerCount = workerCount;
        }

        public Integer getPollIntervalInSeconds() {
            return pollIntervalInSeconds;
        }

        public void setPollIntervalInSeconds(Integer pollIntervalInSeconds) {
            this.pollIntervalInSeconds = pollIntervalInSeconds;
        }

        public Duration getDeleteSucceededJobsAfter() {
            return deleteSucceededJobsAfter;
        }

        public void setDeleteSucceededJobsAfter(Duration deleteSucceededJobsAfter) {
            this.deleteSucceededJobsAfter = deleteSucceededJobsAfter;
        }

        public Duration getDeleteFailedJobsAfter() {
            return deleteFailedJobsAfter;
        }

        public void setDeleteFailedJobsAfter(Duration deleteFailedJobsAfter) {
            this.deleteFailedJobsAfter = deleteFailedJobsAfter;
        }

        public Duration getPermanentlyDeleteDeletedJobsAfter() {
            return permanentlyDeleteDeletedJobsAfter;
        }

        public void setPermanentlyDeleteDeletedJobsAfter(Duration permanentlyDeleteDeletedJobsAfter) {
            this.permanentlyDeleteDeletedJobsAfter = permanentlyDeleteDeletedJobsAfter;
        }
    }

    /**
     * JobRunr dashboard related settings
     */
    public static class Dashboard {

        /**
         * Enables the JobRunr dashboard.
         */
        private boolean enabled = false;

        /**
         * Whether the Dashboard should enable HTTP connections or not
         */
        private boolean enableHttp = false;

        /**
         * The port on which the HTTP Dashboard should run
         */
        private int port = 8000;

        /**
         * The username used to authenticate to the Dashboard.
         * If null, no authentication will be required.
         */
        private String username = null;

        /**
         * The password for the basic authentication which protects the dashboard. WARNING: this is insecure as it is in clear text
         * The password used to authenticate to the Dashboard.
         * If null, no authentication will be required.
         */
        private String password = null;

        /**
         * Whether the Dashboard should enable HTTPS connections or not
         */
        private boolean enableHttps = false;

        /**
         * The port on which the HTTPS Dashboard should run
         */
        private int portHttps = 8001;

        /**
         * The keystore which the Dashboard will use for TLS (https).
         * If null, a self-signed certificate will be generated.
         */
        private Path keyStorePathHttps = null;

        /**
         * The password of the keystore which the Dashboard will use for TLS (https).
         * If null, an empty password will be used.
         */
        private String keyStorePasswordHttps = null;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isHttpEnabled() {
            return enableHttp;
        }

        public void setHttpEnabled(boolean enableHttp) {
            this.enableHttp = enableHttp;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() { return username; }

        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }

        public void setPassword(String password) { this.password = password; }

        public boolean isHttpsEnabled() {
            return enableHttps;
        }

        public void setHttpsEnabled(boolean enableHttps) {
            this.enableHttps = enableHttps;
        }

        public int getPortHttps() {
            return portHttps;
        }

        public void setPortHttps(int portHttps) {
            this.portHttps = portHttps;
        }

        public Path getKeyStorePathHttps() { return keyStorePathHttps; }

        public void setKeyStorePathHttps(Path keyStorePathHttps) { this.keyStorePathHttps = keyStorePathHttps; }

        public String getKeyStorePasswordHttps() { return keyStorePasswordHttps; }

        public void setKeyStorePasswordHttps(String keyStorePasswordHttps) { this.keyStorePasswordHttps = keyStorePasswordHttps; }
    }

    /**
     * JobRunr dashboard related settings
     */
    public static class Database {
        /**
         * Allows to skip the creation of the tables - this means you should add them manually or by database migration tools like FlywayDB.
         */
        private boolean skipCreate = false;

        /**
         * Allows to set the table prefix used by JobRunr
         */
        private String tablePrefix;

        /**
         * An optional named {@link javax.sql.DataSource} to use. Defaults to the 'default' datasource.
         */
        private String datasource;

        public void setSkipCreate(boolean skipCreate) {
            this.skipCreate = skipCreate;
        }

        public boolean isSkipCreate() {
            return skipCreate;
        }

        public String getTablePrefix() {
            return tablePrefix;
        }

        public void setTablePrefix(String tablePrefix) {
            this.tablePrefix = tablePrefix;
        }

        public String getDatasource() {
            return datasource;
        }

        public void setDatasource(String datasource) {
            this.datasource = datasource;
        }
    }
}
