package org.jobrunr.scheduling;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.metadata.DisposableResource;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.exceptions.JobClassNotFoundException;
import org.jobrunr.scheduling.exceptions.JobMethodNotFoundException;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderForTest;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.ZoneId.systemDefault;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

/**
 * Must be public as used as a background job
 */
public class JobStateChangeTest {

    private TestService testService;
    private StorageProviderForTest storageProvider;
    private BackgroundJobServer backgroundJobServer;

    @BeforeEach
    void setUpTests() {
        testService = new TestService();
        testService.reset();
        storageProvider = new StorageProviderForTest(new InMemoryStorageProvider());
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration()
                .andPollIntervalInSeconds(2)
                .andDeleteFailedJobsAfter(Duration.ofSeconds(3))
                .andDeleteSucceededJobsAfter(Duration.ofSeconds(3))
                .andPermanentlyDeleteDeletedJobsAfter(Duration.ofSeconds(3));
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(backgroundJobServerConfiguration)
                .initialize();

        backgroundJobServer = JobRunr.getBackgroundJobServer();
    }

    @AfterEach
    void cleanUp() {
        backgroundJobServer.stop();
    }

    @Test
    void testSucceededThenDeletedThenRemoved() {
        JobId jobId = BackgroundJob.enqueue(() -> System.out.println("this is a test"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);

        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == DELETED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED, DELETED);

        await().atMost(TEN_SECONDS).until(() -> { try { storageProvider.getJobById(jobId); return false; } catch (JobNotFoundException e){ return true; }});
    }

    @Test
    void testFailedThenDeletedThenRemoved() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatFailsWithoutRetries());
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == FAILED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED);

        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == DELETED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED, DELETED);

        await().atMost(TEN_SECONDS).until(() -> { try { storageProvider.getJobById(jobId); return false; } catch (JobNotFoundException e){ return true; }});
    }

    class DisposableTemporaryFile implements DisposableResource {
        private File disposableFile;

        public DisposableTemporaryFile() {
            try {
                disposableFile = File.createTempFile("jobrunr", "gc");
                disposableFile.deleteOnExit();
            } catch (IOException e) {
                throw new IllegalStateException("Unexpected failure creating garbage collecting test file", e);
            }
        }


        @Override
        public void dispose() throws Exception {
            if (!disposableFile.delete())
                throw new IllegalStateException("Unexpected failure garbage collecting test file");
        }

        public boolean exists() {
            return disposableFile.exists();
        }
    }

    @Test
    void testSucceededFinallyRemovedAndGCed() {
        ConcurrentHashMap<String, Object> metadata = new ConcurrentHashMap<>();
        DisposableTemporaryFile disposableResource = new DisposableTemporaryFile();
        metadata.put("disposableResource", disposableResource);

        JobId jobId = BackgroundJob.enqueue(null, () -> System.out.println("this is a test"), metadata);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);

        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == DELETED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED, DELETED);

        assertThat(disposableResource.exists());

        await().atMost(TEN_SECONDS).until(() -> { try { storageProvider.getJobById(jobId); return false; } catch (JobNotFoundException e){ return true; }});

        assertThat(!disposableResource.exists());
    }

    @Test
    void testFailedFinallyRemovedAndGCed() {
        ConcurrentHashMap<String, Object> metadata = new ConcurrentHashMap<>();
        DisposableTemporaryFile disposableResource = new DisposableTemporaryFile();
        metadata.put("disposableResource", disposableResource);

        JobId jobId = BackgroundJob.enqueue(null, () -> testService.doWorkThatFailsWithoutRetries(), metadata);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == FAILED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED);

        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == DELETED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED, DELETED);

        await().atMost(TEN_SECONDS).until(() -> { try { storageProvider.getJobById(jobId); return false; } catch (JobNotFoundException e){ return true; }});

        assertThat(disposableResource.exists());

        await().atMost(TEN_SECONDS).until(() -> { try { storageProvider.getJobById(jobId); return false; } catch (JobNotFoundException e){ return true; }});

        assertThat(!disposableResource.exists());
    }
}

