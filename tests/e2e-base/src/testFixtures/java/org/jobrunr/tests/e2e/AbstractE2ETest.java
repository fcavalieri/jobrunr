package org.jobrunr.tests.e2e;

import org.awaitility.core.ConditionTimeoutException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.configuration.JobRunrConfiguration;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.tests.e2e.services.TestService;
import org.jobrunr.utils.Stopwatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;
import static org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;

public abstract class AbstractE2ETest {

    protected StorageProvider storageProvider;

    protected abstract StorageProvider getStorageProviderForClient();

    protected abstract AbstractBackgroundJobContainer backgroundJobServer();

    public JobRunrConfiguration.JsonMapperKind overrideJsonMapperKind() {
        return null;
    }

    @BeforeEach
    public void setUpJobRunr() {
        storageProvider = getStorageProviderForClient();

        if (overrideJsonMapperKind() != null) {
            JobRunr.configure(overrideJsonMapperKind())
                    .useStorageProvider(storageProvider)
                    .initialize();
        } else {
            JobRunr.configure()
                    .useStorageProvider(storageProvider)
                    .initialize();
        }
    }

    @Test
    void testProcessInBackgroundJobServer() {
        TestService testService = new TestService();
        final JobId jobId = BackgroundJob.enqueue(() -> testService.doWork());
        try {
            with()
                    .pollInterval(FIVE_SECONDS)
                    .await().atMost(45, TimeUnit.SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        } catch (ConditionTimeoutException e) {
            System.out.println("===============================================================");
            System.out.println("======================= Test Timed out ========================");
            System.out.println("===============================================================");
            System.out.println("Server logs: \n" + backgroundJobServer().getLogs() + "\n");
            System.out.println("Job status: \n" + storageProvider.getJobById(jobId) + "\n");
            throw e;
        }
    }

    @Test
    void testEachRecurringJobsRunsOnlyOnceAtTheSameTime() {
        TestService testService = new TestService();
        BackgroundJob.scheduleRecurrently("recurring-job", "*/5 * * * * *", () -> testService.doWorkThatFails());
        try { await().atMost(Duration.ofSeconds(30)).until(() -> false); } catch (Throwable t) {}
        List<Job> allJobs = Arrays.stream(StateName.values()).map(n -> storageProvider.getJobs(n, PageRequest.ascOnUpdatedAt(999))).flatMap(List::stream).collect(Collectors.toList());
        List<Job> recurringJobs = allJobs.stream().filter(j -> j.getState() != StateName.FAILED && j.getState() != StateName.SUCCEEDED && j.getJobName().equals("Recurring-Job-Test")).collect(Collectors.toList());
        assertThat(recurringJobs.size()).isEqualTo(1);
    }

    @Disabled
    @Test
    void performanceTest() {
        TestService testService = new TestService();

        Stream<UUID> workStream = IntStream
                .range(0, 5000)
                .mapToObj((i) -> UUID.randomUUID());

        Stopwatch stopwatch = new Stopwatch();
        try (Stopwatch start = stopwatch.start()) {
            BackgroundJob.enqueue(workStream, uuid -> testService.doWork());
        }
        System.out.println("Time taken to enqueue 5000 jobs: " + stopwatch.duration().getSeconds() + " s");

        try (Stopwatch start = stopwatch.start()) {
            await()
                    .atMost(TEN_SECONDS)
                    .pollInterval(FIVE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> assertThat(storageProvider).hasJobs(SUCCEEDED, 5000));
        }
        System.out.println("Time taken to process 5000 jobs: " + stopwatch.duration().getSeconds() + " s");
    }
}
