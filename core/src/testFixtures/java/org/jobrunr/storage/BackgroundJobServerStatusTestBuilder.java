package org.jobrunr.storage;

import org.jobrunr.server.BackgroundJobServerConfiguration;

import java.time.Duration;

public class BackgroundJobServerStatusTestBuilder {

    private int workerPoolSize = 10;
    private int pollIntervalInSeconds = BackgroundJobServerConfiguration.DEFAULT_POLL_INTERVAL_IN_SECONDS;
    private Duration deleteSucceededJobsAfter = BackgroundJobServerConfiguration.DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION;
    private Duration deleteFailedJobsAfter = BackgroundJobServerConfiguration.DEFAULT_DELETE_FAILED_JOBS_DURATION;
    private Duration permanentlyDeleteDeletedJobsAfter = BackgroundJobServerConfiguration.DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION;
    private boolean started;

    private BackgroundJobServerStatusTestBuilder() {

    }

    public static BackgroundJobServerStatusTestBuilder aFastBackgroundJobServerStatus() {
        return new BackgroundJobServerStatusTestBuilder()
                .withPollIntervalInSeconds(5);
    }

    public static BackgroundJobServerStatusTestBuilder aDefaultBackgroundJobServerStatus() {
        return new BackgroundJobServerStatusTestBuilder();
    }

    public BackgroundJobServerStatusTestBuilder withPollIntervalInSeconds(int pollIntervalInSeconds) {
        this.pollIntervalInSeconds = pollIntervalInSeconds;
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withWorkerSize(int workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
        return this;
    }

    public BackgroundJobServerStatusTestBuilder withIsStarted() {
        this.started = true;
        return this;
    }

    public BackgroundJobServerStatus build() {
        BackgroundJobServerStatus backgroundJobServerStatus = new BackgroundJobServerStatus(workerPoolSize, pollIntervalInSeconds, deleteSucceededJobsAfter, deleteFailedJobsAfter, permanentlyDeleteDeletedJobsAfter);
        if (started) {
            backgroundJobServerStatus.start();
        }
        return backgroundJobServerStatus;
    }
}
