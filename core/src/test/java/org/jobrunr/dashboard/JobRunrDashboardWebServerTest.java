package org.jobrunr.dashboard;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.dashboard.server.http.client.TeenyHttpClient;
import org.jobrunr.dashboard.ui.model.problems.SevereJobRunrExceptionProblem;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.FreePortFinder;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;

abstract class JobRunrDashboardWebServerTest {

    private StorageProvider storageProvider;

    private JobRunrDashboardWebServer dashboardWebServer;
    private TeenyHttpClient http;

    abstract JsonMapper getJsonMapper();
    abstract JobRunrDashboardWebServerConfiguration getDashboardConfiguration();

    @BeforeEach
    void setUpWebServer() {
        final JsonMapper jsonMapper = getJsonMapper();
        final JobRunrDashboardWebServerConfiguration configuration = getDashboardConfiguration();

        storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(jsonMapper));

        dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper, configuration);
        dashboardWebServer.start();

        if (configuration.enableHttps)
            http = new TeenyHttpClient("https://localhost:" + configuration.portHttps);
        else
            http = new TeenyHttpClient("http://localhost:" + configuration.port);
    }

    @AfterEach
    void stopWebServer() {
        dashboardWebServer.stop();
        storageProvider.close();
    }

    @Test
    void testGetJobById_ForEnqueuedJob() {
        final Job job = anEnqueuedJob().build();
        final Job savedJob = storageProvider.save(job);

        HttpResponse<String> getResponse = http.get("/api/jobs/%s", savedJob.getId());
        assertThat(getResponse).hasStatusCode(200);
    }

    @Test
    void testGetJobById_ForFailedJob() {
        final Job job = aFailedJobWithRetries().build();
        final Job savedJob = storageProvider.save(job);

        HttpResponse<String> getResponse = http.get("/api/jobs/%s", savedJob.getId());
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/getJobById_ForFailedJob.json");
    }

    @Test
    void testRequeueJob() {
        final Job job = aFailedJobWithRetries().build();
        final Job savedJob = storageProvider.save(job);

        HttpResponse<String> deleteResponse = http.post("/api/jobs/%s/requeue", savedJob.getId());
        assertThat(deleteResponse).hasStatusCode(204);

        assertThat(storageProvider.getJobById(job.getId())).hasState(StateName.ENQUEUED);
    }

    @Test
    void testDeleteJob() {
        final Job job = aFailedJobWithRetries().build();
        final Job savedJob = storageProvider.save(job);

        HttpResponse<String> deleteResponse = http.delete("/api/jobs/%s", savedJob.getId());
        assertThat(deleteResponse).hasStatusCode(204);

        HttpResponse<String> getResponse = http.get("/api/jobs/%s", savedJob.getId());
        assertThat(getResponse).hasStatusCode(200);
        assertThat(storageProvider.getJobById(savedJob.getId())).hasState(StateName.DELETED);
    }

    @Test
    void testGetJobById_JobNotFoundReturns404() {
        HttpResponse<String> getResponse = http.get("/api/jobs/%s", randomUUID());
        assertThat(getResponse).hasStatusCode(404);
    }

    @Test
    void testFindJobsByState() {
        storageProvider.save(anEnqueuedJob().build());

        HttpResponse<String> getResponse = http.get("/api/jobs?state=ENQUEUED");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/findJobsByState.json");
    }

    @Test
    void testGetProblems() {
        storageProvider.save(aJob().withJobDetails(methodThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, ChronoUnit.DAYS))).build());

        HttpResponse<String> getResponse = http.get("/api/problems");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/problems-job-not-found.json");
    }

    @Test
    void testDeleteProblem() {
        storageProvider.saveMetadata(new JobRunrMetadata(SevereJobRunrException.class.getSimpleName(), "some id", "some value"));

        HttpResponse<String> getResponseBeforeDelete = http.get("/api/problems");
        assertThat(getResponseBeforeDelete)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/problems-severe-jobrunr-problem.json");


        http.delete("/api/problems/" + SevereJobRunrExceptionProblem.PROBLEM_TYPE);
        HttpResponse<String> getResponseAfterDelete = http.get("/api/problems");
        assertThat(getResponseAfterDelete)
                .hasStatusCode(200)
                .hasJsonBody("[]");
    }

    @Test
    void testGetRecurringJobs() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-2").withName("Generate sales reports").build());

        HttpResponse<String> getResponse = http.get("/api/recurring-jobs");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/getRecurringJobs.json");
    }

    @Test
    void testDeleteRecurringJob() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-2").withName("Generate sales reports").build());

        HttpResponse<String> deleteResponse = http.delete("/api/recurring-jobs/%s", "recurring-job-1");
        assertThat(deleteResponse).hasStatusCode(204);
        assertThat(storageProvider.getRecurringJobs()).hasSize(1);
    }

    @Test
    void testDeleteReadOnlyRecurringJob() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").withDeletableFromDashboard(false).build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-2").withName("Generate sales reports").build());

        HttpResponse<String> deleteResponse = http.delete("/api/recurring-jobs/%s", "recurring-job-1");
        assertThat(deleteResponse).hasStatusCode(409);
        assertThat(storageProvider.getRecurringJobs()).hasSize(2);
    }

    @Test
    void testEnableDisableRecurringJob() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").build());
        assertThat(storageProvider.getRecurringJobs()).hasSize(1);
        assertThat(storageProvider.getRecurringJobs().stream().findFirst().get().isEnabled());

        HttpResponse<String> disableResponse = http.post("/api/recurring-jobs/%s/disable", "recurring-job-1");
        assertThat(disableResponse).hasStatusCode(204);
        assertThat(storageProvider.getRecurringJobs()).hasSize(1);
        assertThat(!storageProvider.getRecurringJobs().stream().findFirst().get().isEnabled());

        HttpResponse<String> enableResponse = http.post("/api/recurring-jobs/%s/enable", "recurring-job-1");
        assertThat(enableResponse).hasStatusCode(204);
        assertThat(storageProvider.getRecurringJobs()).hasSize(1);
        assertThat(storageProvider.getRecurringJobs().stream().findFirst().get().isEnabled());
    }

    @Test
    void testTriggerRecurringJob() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").withCronExpression(Cron.minutely()).build());
        HttpResponse<String> triggerResponse = http.post("/api/recurring-jobs/%s/trigger", "recurring-job-1");
        assertThat(triggerResponse).hasStatusCode(204);
        HttpResponse<String> triggerResponse2 = http.post("/api/recurring-jobs/%s/trigger", "recurring-job-1");
        assertThat(triggerResponse2).hasStatusCode(204);

        List<Job> allJobs = Arrays.stream(StateName.values()).map(n -> storageProvider.getJobs(n, PageRequest.ascOnUpdatedAt(999))).flatMap(List::stream).collect(Collectors.toList());
        assertThat(allJobs).hasSize(1);

        RecurringJob recurringJob = aDefaultRecurringJob().withId("recurring-job-2").withName("Import sales data").build();
        storageProvider.saveRecurringJob(recurringJob);
        storageProvider.save(aJob().withName("a scheduled job").withState(new ScheduledState(now().minusSeconds(1), recurringJob)).build());
        HttpResponse<String> triggerResponse3 = http.post("/api/recurring-jobs/%s/trigger", "recurring-job-1");
        assertThat(triggerResponse3).hasStatusCode(204);
        HttpResponse<String> triggerResponse4 = http.post("/api/recurring-jobs/%s/trigger", "recurring-job-1");
        assertThat(triggerResponse4).hasStatusCode(204);

        List<Job> allJobs2 = Arrays.stream(StateName.values()).map(n -> storageProvider.getJobs(n, PageRequest.ascOnUpdatedAt(999))).flatMap(List::stream).collect(Collectors.toList());
        assertThat(allJobs2).hasSize(2);

        Instant nextRun = storageProvider.getRecurringJobs().stream().filter(j -> j.getId().equals("recurring-job-1")).findFirst().get().getNextRun();
        try { await().atMost(Duration.ofSeconds(30)).until(() -> false); } catch (Exception e) {}
        await().atMost(Duration.ofSeconds(60)).until(() -> nextRun.isBefore(Instant.now().plusSeconds(30)));

        List<Job> allJobs3 = Arrays.stream(StateName.values()).map(n -> storageProvider.getJobs(n, PageRequest.ascOnUpdatedAt(999))).flatMap(List::stream).collect(Collectors.toList());
        assertThat(allJobs3).hasSize(2);
    }

    @Test
    void testGetBackgroundJobServers() {
        final BackgroundJobServerStatus serverStatus = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        storageProvider.announceBackgroundJobServer(serverStatus);

        HttpResponse<String> getResponse = http.get("/api/servers");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/getBackgroundJobServers.json");
    }
}