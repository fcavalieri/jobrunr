package org.jobrunr.scheduling;

import io.micronaut.inject.ExecutableMethod;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.micronaut.annotations.Recurring;
import org.jobrunr.scheduling.cron.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;

import static java.util.Collections.emptyList;

public class JobRunrRecurringJobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrRecurringJobScheduler.class);

    private final JobScheduler jobScheduler;

    public JobRunrRecurringJobScheduler(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    public void schedule(ExecutableMethod<?, ?> method) {
        if (method.getTargetMethod().getParameterCount() > 0) {
            throw new IllegalStateException("Methods annotated with " + Recurring.class.getName() + " can not have parameters.");
        }

        String id = method.stringValue(Recurring.class, "id").orElse(null);
        String cron = method.stringValue(Recurring.class, "cron").orElseThrow(() -> new IllegalArgumentException("Cron attribute is required"));

        if (Recurring.CRON_DISABLED.equals(cron)) {
            if (id == null) {
                LOGGER.warn("You are trying to disable a recurring job using placeholders but did not define an id.");
            } else {
                jobScheduler.delete(id);
            }
        } else {
            JobDetails jobDetails = new JobDetails(method.getTargetMethod().getDeclaringClass().getName(), null, method.getTargetMethod().getName(), emptyList());
            ZoneId zoneId = method.stringValue(Recurring.class, "zone").map(ZoneId::of).orElse(ZoneId.systemDefault());
            jobScheduler.scheduleRecurrently(id, jobDetails, CronExpression.create(cron), zoneId);
        }
    }
}
