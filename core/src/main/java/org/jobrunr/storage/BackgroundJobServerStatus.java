package org.jobrunr.storage;

import org.jobrunr.server.jmx.BackgroundJobServerStatusMBean;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class BackgroundJobServerStatus implements BackgroundJobServerStatusMBean {

    private final UUID id;
    private final int workerPoolSize;
    private final int pollIntervalInSeconds;
    private final Duration deleteSucceededJobsAfter;
    //JobRunrPlus: support automatic deletion of failed jobs
    private final Duration deleteFailedJobsAfter;
    private final Duration permanentlyDeleteDeletedJobsAfter;
    private final Instant firstHeartbeat;
    private final Instant lastHeartbeat;
    private final Boolean running;
    private final Long systemTotalMemory;
    private final Long systemFreeMemory;
    private final Double systemCpuLoad;
    private final Long processMaxMemory;
    private final Long processFreeMemory;
    private final Long processAllocatedMemory;
    private final Double processCpuLoad;

    //JobRunrPlus: support automatic deletion of failed jobs
    public BackgroundJobServerStatus(int workerPoolSize, int pollIntervalInSeconds, Duration deleteSucceededJobsAfter, Duration deleteFailedJobsAfter, Duration permanentlyDeleteDeletedJobsAfter) {
        this(UUID.randomUUID(), workerPoolSize, pollIntervalInSeconds, deleteSucceededJobsAfter, deleteFailedJobsAfter, permanentlyDeleteDeletedJobsAfter, null, null, false, null, null, null, null, null, null, null);
    }

    //JobRunrPlus: support automatic deletion of failed jobs
    public BackgroundJobServerStatus(UUID id, int workerPoolSize, int pollIntervalInSeconds, Duration deleteSucceededJobsAfter, Duration deleteFailedJobsAfter, Duration permanentlyDeleteDeletedJobsAfter, Instant firstHeartbeat, Instant lastHeartbeat, boolean isRunning, Long systemTotalMemory, Long systemFreeMemory, Double systemCpuLoad, Long processMaxMemory, Long processFreeMemory, Long processAllocatedMemory, Double processCpuLoad) {
        this.id = id;
        this.workerPoolSize = workerPoolSize;
        this.pollIntervalInSeconds = pollIntervalInSeconds;
        this.deleteSucceededJobsAfter = deleteSucceededJobsAfter;
        //JobRunrPlus: support automatic deletion of failed jobs
        this.deleteFailedJobsAfter = deleteFailedJobsAfter;
        this.permanentlyDeleteDeletedJobsAfter = permanentlyDeleteDeletedJobsAfter;
        this.firstHeartbeat = firstHeartbeat;
        this.lastHeartbeat = lastHeartbeat;
        this.running = isRunning;
        this.systemTotalMemory = systemTotalMemory;
        this.systemFreeMemory = systemFreeMemory;
        this.systemCpuLoad = systemCpuLoad;
        this.processMaxMemory = processMaxMemory;
        this.processFreeMemory = processFreeMemory;
        this.processAllocatedMemory = processAllocatedMemory;
        this.processCpuLoad = processCpuLoad;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public int getWorkerPoolSize() {
        return workerPoolSize;
    }

    @Override
    public int getPollIntervalInSeconds() {
        return pollIntervalInSeconds;
    }

    @Override
    public Duration getDeleteSucceededJobsAfter() {
        return deleteSucceededJobsAfter;
    }

    //JobRunrPlus: support automatic deletion of failed jobs
    @Override
    public Duration getDeleteFailedJobsAfter() {
        return deleteFailedJobsAfter;
    }

    @Override
    public Duration getPermanentlyDeleteDeletedJobsAfter() {
        return permanentlyDeleteDeletedJobsAfter;
    }

    @Override
    public Instant getFirstHeartbeat() {
        return firstHeartbeat;
    }

    @Override
    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public Long getSystemTotalMemory() {
        return systemTotalMemory;
    }

    @Override
    public Long getSystemFreeMemory() {
        return systemFreeMemory;
    }

    @Override
    public Double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    @Override
    public Long getProcessMaxMemory() {
        return processMaxMemory;
    }

    @Override
    public Long getProcessFreeMemory() {
        return processFreeMemory;
    }

    @Override
    public Long getProcessAllocatedMemory() {
        return processAllocatedMemory;
    }

    @Override
    public Double getProcessCpuLoad() {
        return processCpuLoad;
    }
}
