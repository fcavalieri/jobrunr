package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.http.RestHttpHandler;
import org.jobrunr.dashboard.server.http.handlers.HttpRequestHandler;
import org.jobrunr.dashboard.ui.model.RecurringJobUIModel;
import org.jobrunr.dashboard.ui.model.VersionUIModel;
import org.jobrunr.dashboard.ui.model.problems.ProblemsManager;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JobRunrApiHandler extends RestHttpHandler {

    private final StorageProvider storageProvider;
    private final ProblemsManager problemsManager;

    public JobRunrApiHandler(StorageProvider storageProvider, JsonMapper jsonMapper) {
        super("/api", jsonMapper);
        this.storageProvider = storageProvider;
        this.problemsManager = new ProblemsManager(storageProvider);

        get("/jobs", findJobByState());

        get("/jobs/:id", getJobById());
        delete("/jobs/:id", deleteJobById());
        post("/jobs/:id/requeue", requeueJobById());

        get("/problems", getProblems());
        delete("/problems/:type", deleteProblemByType());

        get("/recurring-jobs", getRecurringJobs());
        delete("/recurring-jobs/:id", deleteRecurringJob());
        post("/recurring-jobs/:id/trigger", triggerRecurringJob());
        post("/recurring-jobs/:id/enable", enableRecurringJob());
        post("/recurring-jobs/:id/disable", disableRecurringJob());

        get("/servers", getBackgroundJobServers());
        get("/version", getVersion());

        withExceptionMapping(JobNotFoundException.class, (exc, resp) -> resp.statusCode(404));
    }

    private HttpRequestHandler getJobById() {
        return (request, response) -> response.asJson(storageProvider.getJobById(request.param(":id", UUID.class)));
    }

    private HttpRequestHandler deleteJobById() {
        return (request, response) -> {
            final Job job = storageProvider.getJobById(request.param(":id", UUID.class));
            job.delete("Job deleted via Dashboard");
            storageProvider.save(job);
            response.statusCode(204);
        };
    }

    private HttpRequestHandler requeueJobById() {
        return (request, response) -> {
            final Job job = storageProvider.getJobById(request.param(":id", UUID.class));
            job.enqueue();
            storageProvider.save(job);
            response.statusCode(204);
        };
    }

    private HttpRequestHandler findJobByState() {
        return (request, response) ->
                response.asJson(
                        storageProvider.getJobPage(
                                request.queryParam("state", StateName.class, StateName.ENQUEUED),
                                request.fromQueryParams(PageRequest.class)
                        ));
    }

    private HttpRequestHandler getProblems() {
        return (request, response) -> response.asJson(problemsManager.getProblems());
    }

    private HttpRequestHandler deleteProblemByType() {
        return (request, response) -> {
            problemsManager.dismissProblemOfType(request.param(":type", String.class));
            response.statusCode(204);
        };
    }

    private HttpRequestHandler getRecurringJobs() {
        return (request, response) -> {
            final List<RecurringJobUIModel> recurringJobUIModels = storageProvider
                    .getRecurringJobs()
                    .stream()
                    .map(RecurringJobUIModel::new)
                    .collect(Collectors.toList());
            response.asJson(recurringJobUIModels);
        };
    }

    private HttpRequestHandler deleteRecurringJob() {
        return (request, response) -> {
            if (storageProvider.getRecurringJobs().stream().anyMatch(j -> j.getId().equals(request.param(":id")) && !j.isDeletableFromDashboard())) {
                response.statusCode(409);
            } else {
                storageProvider.deleteRecurringJob(request.param(":id"));
                response.statusCode(204);
            }
        };
    }

    private HttpRequestHandler triggerRecurringJob() {
        return (request, response) -> {
            final RecurringJob recurringJob = storageProvider.getRecurringJobs()
                    .stream()
                    .filter(rj -> request.param(":id").equals(rj.getId()))
                    .findFirst()
                    .orElseThrow(() -> new JobNotFoundException(request.param(":id")));
            if (!storageProvider.recurringJobExists(recurringJob.getId(), StateName.SCHEDULED, StateName.ENQUEUED, StateName.PROCESSING)) {
                final Job job = recurringJob.toImmediatelyScheduledJob();
                storageProvider.save(job);
            }
            response.statusCode(204);
        };
    }

    private HttpRequestHandler enableRecurringJob() {
        return (request, response) -> {
            final RecurringJob recurringJob = storageProvider.getRecurringJobs()
                    .stream()
                    .filter(rj -> request.param(":id").equals(rj.getId()))
                    .findFirst()
                    .orElseThrow(() -> new JobNotFoundException(request.param(":id")));

            recurringJob.setEnabled(true);
            storageProvider.saveRecurringJob(recurringJob);
            response.statusCode(204);
        };
    }

    private HttpRequestHandler disableRecurringJob() {
        return (request, response) -> {
            final RecurringJob recurringJob = storageProvider.getRecurringJobs()
                    .stream()
                    .filter(rj -> request.param(":id").equals(rj.getId()))
                    .findFirst()
                    .orElseThrow(() -> new JobNotFoundException(request.param(":id")));

            recurringJob.setEnabled(false);
            storageProvider.saveRecurringJob(recurringJob);
            response.statusCode(204);
        };
    }

    private HttpRequestHandler getBackgroundJobServers() {
        return (request, response) -> response.asJson(storageProvider.getBackgroundJobServers());
    }

    private HttpRequestHandler getVersion() {
        return (request, response) -> response.asJson(new VersionUIModel());
    }
}
