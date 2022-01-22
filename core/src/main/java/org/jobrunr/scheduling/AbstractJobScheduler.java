package org.jobrunr.scheduling;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.filters.JobFilterUtils;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.emptyList;

public class AbstractJobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJobScheduler.class);

    private final StorageProvider storageProvider;
    private final JobFilterUtils jobFilterUtils;
    protected static final int BATCH_SIZE = 5000;

    /**
     * Creates a new AbstractJobScheduler using the provided storageProvider
     *
     * @param storageProvider the storageProvider to use
     */
    public AbstractJobScheduler(StorageProvider storageProvider) {
        this(storageProvider, emptyList());
    }

    /**
     * Creates a new AbstractJobScheduler using the provided storageProvider and the list of JobFilters that will be used for every background job
     *
     * @param storageProvider the storageProvider to use
     * @param jobFilters      list of jobFilters that will be used for every job
     */
    public AbstractJobScheduler(StorageProvider storageProvider, List<JobFilter> jobFilters) {
        if (storageProvider == null)
            throw new IllegalArgumentException("A JobStorageProvider is required to use the JobScheduler. Please see the documentation on how to setup a JobStorageProvider.");
        this.storageProvider = storageProvider;
        this.jobFilterUtils = new JobFilterUtils(new JobDefaultFilters(jobFilters));
    }

    /**
     * @see #delete(UUID)
     */
    public void delete(JobId jobId) {
        this.delete(jobId.asUUID());
    }

    /**
     * @see #delete(UUID, String)
     */
    public void delete(JobId jobId, String reason) {
        this.delete(jobId.asUUID(), reason);
    }

    /**
     * Deletes a job and sets its state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id the id of the job
     */
    public void delete(UUID id) {
        delete(id, "Deleted via JobScheduler API");
    }

    /**
     * Deletes a job and sets its state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id     the id of the job
     * @param reason the reason why the job is deleted.
     */
    public void delete(UUID id, String reason) {
        final Job jobToDelete = storageProvider.getJobById(id);
        jobToDelete.delete(reason);
        jobFilterUtils.runOnStateElectionFilter(jobToDelete);
        final Job deletedJob = storageProvider.save(jobToDelete);
        jobFilterUtils.runOnStateAppliedFilters(deletedJob);
        LOGGER.debug("Deleted Job with id {}", deletedJob.getId());
    }

    /**
     * Triggers the recurring job based on the given id.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.delete("my-recurring-job"));
     * }</pre>
     *
     * @param id the id of the recurring job to trigger
     * @return the id of the Job
     */
    public JobId trigger(String id) {
        final RecurringJob recurringJob = storageProvider.getRecurringJobById(id);
        if (!storageProvider.recurringJobExists(recurringJob.getId(), StateName.SCHEDULED, StateName.ENQUEUED, StateName.PROCESSING)) {
            final Job job = recurringJob.toImmediatelyScheduledJob();
            return new JobId(storageProvider.save(job).getId());
        }
        return null;
    }

    /**
     * Disables the recurring job based on the given id.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.disable("my-recurring-job"));
     * }</pre>
     *
     * @param id the id of the recurring job to disable
     */
    public void enable(String id) {
        final RecurringJob recurringJob = storageProvider.getRecurringJobById(id);

        recurringJob.setEnabled(true);
        storageProvider.saveRecurringJob(recurringJob);
    }

    /**
     * Disables the recurring job based on the given id.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.disable("my-recurring-job"));
     * }</pre>
     *
     * @param id the id of the recurring job to disable
     */
    public void disable(String id) {
        final RecurringJob recurringJob = storageProvider.getRecurringJobById(id);

        recurringJob.setEnabled(false);
        storageProvider.saveRecurringJob(recurringJob);
    }

    /**
     * Deletes the recurring job based on the given id.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.delete("my-recurring-job"));
     * }</pre>
     *
     * @param id the id of the recurring job to delete
     */
    public void delete(String id) {
        this.storageProvider.deleteRecurringJob(id);
    }





    /**
     * Utility method to register the shutdown of JobRunr in various containers - it is even automatically called by Spring Framework.
     * Note that this will stop the BackgroundJobServer, the Dashboard and the StorageProvider. JobProcessing will stop and enqueueing new jobs will fail.
     */
    public void shutdown() {
        JobRunr.destroy();
    }

    JobId enqueue(UUID id, JobDetails jobDetails) {
        return saveJob(new Job(id, jobDetails));
    }

    JobId enqueue(UUID id, JobDetails jobDetails, ConcurrentMap<String, Object> metadata) {
        return saveJob(new Job(id, jobDetails, metadata));
    }

    JobId schedule(UUID id, Instant scheduleAt, JobDetails jobDetails) {
        return saveJob(new Job(id, jobDetails, new ScheduledState(scheduleAt)));
    }

    JobId schedule(UUID id, Instant scheduleAt, JobDetails jobDetails, ConcurrentMap<String, Object> metadata) {
        return saveJob(new Job(id, jobDetails, new ScheduledState(scheduleAt), metadata));
    }

    String scheduleRecurrently(String id, JobDetails jobDetails, CronExpression cronExpression, ZoneId zoneId) {
        return doScheduleRecurrently(id, jobDetails, cronExpression, zoneId, null, null);
    }

    String scheduleRecurrently(String id, JobDetails jobDetails, CronExpression cronExpression, ZoneId zoneId, boolean enabled, boolean deletableFromDashboard) {
        return doScheduleRecurrently(id, jobDetails, cronExpression, zoneId, enabled, deletableFromDashboard);
    }

    private String doScheduleRecurrently(String id, JobDetails jobDetails, CronExpression cronExpression, ZoneId zoneId, Boolean enabled, Boolean deletableFromDashboard) {
        final RecurringJob recurringJob = new RecurringJob(id, jobDetails, cronExpression, zoneId);
        if (enabled != null)
            recurringJob.setEnabled(enabled);
        if (deletableFromDashboard != null)
            recurringJob.setDeletableFromDashboard(deletableFromDashboard);
        jobFilterUtils.runOnCreatingFilter(recurringJob);
        RecurringJob savedRecurringJob = this.storageProvider.saveRecurringJob(recurringJob);
        jobFilterUtils.runOnCreatedFilter(recurringJob);
        return savedRecurringJob.getId();
    }

    JobId saveJob(Job job) {
        try {
            jobFilterUtils.runOnCreatingFilter(job);
            Job savedJob = this.storageProvider.save(job);
            jobFilterUtils.runOnCreatedFilter(savedJob);
            LOGGER.debug("Created Job with id {}", job.getId());
        } catch (ConcurrentJobModificationException e) {
            LOGGER.info("Skipped Job with id {} as it already exists", job.getId());
        }
        return new JobId(job.getId());
    }

    List<Job> saveJobs(List<Job> jobs) {
        jobFilterUtils.runOnCreatingFilter(jobs);
        final List<Job> savedJobs = this.storageProvider.save(jobs);
        jobFilterUtils.runOnCreatedFilter(savedJobs);
        return savedJobs;
    }
}
