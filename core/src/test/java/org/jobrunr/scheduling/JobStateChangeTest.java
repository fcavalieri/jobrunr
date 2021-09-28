package org.jobrunr.scheduling;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mongodb.assertions.Assertions;
import org.assertj.core.util.Files;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProviderForTest;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;
import static org.mockito.ArgumentMatchers.any;

/**
 * Must be public as used as a background job
 */
public class JobStateChangeTest {

    private TestService testService;
    private StorageProviderForTest storageProvider;
    private BackgroundJobServer backgroundJobServer;
    private ListAppender<ILoggingEvent> logger;

    @BeforeEach
    void setUpTests() {
        testService = new TestService();
        testService.reset();
        storageProvider = new StorageProviderForTest(new InMemoryStorageProvider());
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration()
                .andPollIntervalInSeconds(5)
                .andDeleteFailedJobsAfter(Duration.ofSeconds(3))
                .andDeleteSucceededJobsAfter(Duration.ofSeconds(3))
                .andPermanentlyDeleteDeletedJobsAfter(Duration.ofSeconds(3));
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(backgroundJobServerConfiguration)
                .initialize();

        backgroundJobServer = JobRunr.getBackgroundJobServer();
        logger = LoggerAssert.initFor(storageProvider.getStorageProvider());
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

    @Test
    void testSucceededFinallyRemovedAndGCed() {
        ConcurrentHashMap<String, Object> metadata = new ConcurrentHashMap<>();
        File temporaryFile = Files.newTemporaryFile();
        temporaryFile.deleteOnExit();
        DisposableTemporaryFile disposableResource = new DisposableTemporaryFile(temporaryFile, false);
        metadata.put("disposableResource", disposableResource);
        Assertions.assertTrue(disposableResource.exists());

        JobId jobId = BackgroundJob.enqueue(null, () -> System.out.println("this is a test"), metadata);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);

        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == DELETED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED, DELETED);

        Assertions.assertTrue(disposableResource.exists());

        await().atMost(TEN_SECONDS).until(() -> { try { storageProvider.getJobById(jobId); return false; } catch (JobNotFoundException e){ return true; }});

        Assertions.assertFalse(disposableResource.exists());
    }

    @Test
    void testFailedFinallyRemovedAndGCed() {
        ConcurrentHashMap<String, Object> metadata = new ConcurrentHashMap<>();
        File temporaryFile = Files.newTemporaryFile();
        temporaryFile.deleteOnExit();
        DisposableTemporaryFile disposableResource = new DisposableTemporaryFile(temporaryFile, false);
        metadata.put("disposableResource", disposableResource);
        Assertions.assertTrue(disposableResource.exists());

        JobId jobId = BackgroundJob.enqueue(null, () -> testService.doWorkThatFailsWithoutRetries(), metadata);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == FAILED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED);

        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == DELETED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED, DELETED);

        await().atMost(TEN_SECONDS).until(() -> { try { storageProvider.getJobById(jobId); return false; } catch (JobNotFoundException e){ return true; }});

        Assertions.assertFalse(disposableResource.exists());
    }

    @Test
    void testGCFailureLogged() {
        ConcurrentHashMap<String, Object> metadata = new ConcurrentHashMap<>();
        File temporaryFile = Files.newTemporaryFile();
        temporaryFile.deleteOnExit();
        DisposableTemporaryFile disposableResource = new DisposableTemporaryFile(temporaryFile, true);
        metadata.put("disposableResource", disposableResource);
        Assertions.assertTrue(disposableResource.exists());

        JobId jobId = BackgroundJob.enqueue(null, () -> System.out.println("this is a test"), metadata);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);

        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == DELETED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED, DELETED);

        Assertions.assertTrue(disposableResource.exists());

        await().atMost(TEN_SECONDS).until(() -> { try { storageProvider.getJobById(jobId); return false; } catch (JobNotFoundException e){ return true; }});
        await().atMost(TEN_SECONDS).untilAsserted(() -> assertThat(logger).hasErrorMessageContaining("Unit Test Failure"));
        Assertions.assertFalse(disposableResource.exists());
    }

}

