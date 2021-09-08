package org.jobrunr.storage.nosql.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.MarkLogicBindingException;
import com.marklogic.client.MarkLogicIOException;
import com.marklogic.client.MarkLogicInternalException;
import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.Transaction;
import com.marklogic.client.document.DocumentPatchBuilder;
import com.marklogic.client.io.marker.DocumentPatchHandle;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.bson.Document;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobListVersioner;
import org.jobrunr.jobs.JobVersioner;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.AbstractStorageProvider;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.marklogic.mapper.MarklogicBackgroundJobServerStatus;
import org.jobrunr.storage.nosql.marklogic.mapper.MarklogicJob;
import org.jobrunr.storage.nosql.marklogic.mapper.MarklogicMetadata;
import org.jobrunr.storage.nosql.marklogic.mapper.MarklogicRecurringJob;
import org.jobrunr.utils.resilience.RateLimiter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class MarklogicStorageProvider extends AbstractStorageProvider implements NoSqlStorageProvider {

    private final DatabaseClient databaseClient;
    private MarklogicWrapper marklogicWrapper;
    private StructuredQueryBuilder queryBuilder;
    private JobMapper jobMapper;

    public MarklogicStorageProvider(String host, int port) {
        this(DatabaseClientFactory.newClient(host, port));
    }

    public MarklogicStorageProvider(String host, int port, String database) {
        this(DatabaseClientFactory.newClient(host, port, database));
    }
    public MarklogicStorageProvider(String host, int port, String username, String password) {
        this(DatabaseClientFactory.newClient(host, port, new DatabaseClientFactory.DigestAuthContext(username, password)));
    }

    public MarklogicStorageProvider(String host, int port, String database, String username, String password) {
        this(DatabaseClientFactory.newClient(host, port, database, new DatabaseClientFactory.DigestAuthContext(username, password)));
    }

    public MarklogicStorageProvider(DatabaseClient databaseClient) {
        this(databaseClient, rateLimit().at1Request().per(SECOND));
    }

    public MarklogicStorageProvider(DatabaseClient databaseClient, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.databaseClient = databaseClient;
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
        this.marklogicWrapper = new MarklogicWrapper(databaseClient, jobMapper.getJsonMapper());
        this.queryBuilder = new StructuredQueryBuilder();

        runMigrations();
    }

    @Override
    public JobMapper getJobMapper() {
        return jobMapper;
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        MarklogicBackgroundJobServerStatus marklogicBackgroundJobServerStatus = new MarklogicBackgroundJobServerStatus(serverStatus);
        marklogicWrapper.putDocument(
                StorageProviderUtils.BackgroundJobServers.NAME,
                serverStatus.getId().toString(),
                marklogicBackgroundJobServerStatus);
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        DocumentPatchBuilder patchBuilder = databaseClient.newJSONDocumentManager().newPatchBuilder();
        boolean documentWasPresent = marklogicWrapper.patchDocumentIfPresent(
                StorageProviderUtils.BackgroundJobServers.NAME,
                serverStatus.getId().toString(),
                MarklogicBackgroundJobServerStatus.toUpdateDocument(serverStatus, patchBuilder));
        if (!documentWasPresent)
            throw new ServerTimedOutException(serverStatus, new StorageException("BackgroundJobServer with id " + serverStatus.getId() + " was not found"));

        MarklogicBackgroundJobServerStatus document = marklogicWrapper.loadDocumentIfPresent(
                StorageProviderUtils.BackgroundJobServers.NAME,
                serverStatus.getId().toString(),
                MarklogicBackgroundJobServerStatus.class);
        return document != null && document.isRunning();
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        marklogicWrapper.deleteDocument(
                StorageProviderUtils.BackgroundJobServers.NAME,
                serverStatus.getId().toString());
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        List<MarklogicBackgroundJobServerStatus> backgroundJobServerStatuses = marklogicWrapper.loadAllDocumentsInDirectory(
                StorageProviderUtils.BackgroundJobServers.NAME,
                MarklogicBackgroundJobServerStatus.class);
        return backgroundJobServerStatuses
                .stream()
                .map(MarklogicBackgroundJobServerStatus::toBackgroundJobServerStatus)
                .sorted(Comparator.comparing(BackgroundJobServerStatus::getFirstHeartbeat))
                .collect(toList());
    }

    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        return getBackgroundJobServers()
                .stream()
                .map(BackgroundJobServerStatus::getId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.BackgroundJobServers.NAME + "/"),
                queryBuilder.range(
                        queryBuilder.jsonProperty(StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT),
                        "long",
                        StructuredQueryBuilder.Operator.LT,
                        "" + heartbeatOlderThan.toEpochMilli())
        );
        List<String> urisToDelete =  marklogicWrapper.queryDocumentURIs(query);
        urisToDelete.forEach(uri -> marklogicWrapper.deleteDocumentByURI(uri));
        return urisToDelete.size();
    }

    @Override
    public void saveMetadata(JobRunrMetadata metadata) {
        MarklogicMetadata metadataMarklogicDocument = new MarklogicMetadata(metadata);
        marklogicWrapper.putDocument(StorageProviderUtils.Metadata.NAME, metadata.getId(), metadataMarklogicDocument);
        notifyMetadataChangeListeners();
    }

    @Override
    public List<JobRunrMetadata> getMetadata(String name) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Metadata.NAME + "/"),
                queryBuilder.value(queryBuilder.jsonProperty(StorageProviderUtils.Metadata.FIELD_NAME), name));
        List<MarklogicMetadata> results = marklogicWrapper.queryDocuments(query, MarklogicMetadata.class);
        return results
                .stream()
                .map(MarklogicMetadata::toJobRunrMetadata)
                .collect(toList());
    }

    @Override
    public JobRunrMetadata getMetadata(String name, String owner) {
        MarklogicMetadata document = marklogicWrapper.loadDocumentIfPresent(
                StorageProviderUtils.Metadata.NAME,
                JobRunrMetadata.toId(name, owner),
                MarklogicMetadata.class);
        return document.toJobRunrMetadata();
    }

    @Override
    public void deleteMetadata(String name) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Metadata.NAME + "/"),
                queryBuilder.value(queryBuilder.jsonProperty(StorageProviderUtils.Metadata.FIELD_NAME), name)
        );
        List<String> urisToDelete =  marklogicWrapper.queryDocumentURIs(query);
        urisToDelete.forEach(uri -> marklogicWrapper.deleteDocumentByURI(uri));
        long deletedCount = urisToDelete.size();
        notifyMetadataChangeListeners(deletedCount > 0);
    }

    @Override
    public Job save(Job job) {
        try (final JobVersioner jobVersioner = new JobVersioner(job)) {
            Transaction transaction = databaseClient.openTransaction();
            if (jobVersioner.isNewJob()) {
                if (marklogicWrapper.existsDocument(
                        StorageProviderUtils.Jobs.NAME,
                        job.getId().toString(),
                        transaction)) {
                    throw new ConcurrentJobModificationException(job);
                }
                marklogicWrapper.putJob(job, jobMapper, transaction);
            } else {
                marklogicWrapper.patchJob(job, jobMapper, transaction);
            }
            transaction.commit();
            jobVersioner.commitVersion();
        } catch (MarkLogicIOException | MarkLogicServerException | MarkLogicBindingException | MarkLogicInternalException e) {
            throw new StorageException(e);
        }
        notifyJobStatsOnChangeListeners();
        return job;
    }

    @Override
    public int deletePermanently(UUID id) {
        MarklogicJob document = marklogicWrapper.loadDocumentIfPresent(
                StorageProviderUtils.Jobs.NAME,
                id.toString(),
                MarklogicJob.class);
        if (document != null) {
            Job job = document.toJob(jobMapper);
            marklogicWrapper.deleteDocument(StorageProviderUtils.Jobs.NAME, job.getId().toString());
            disposeJobResources(job.getMetadata());
            notifyJobStatsOnChangeListeners();
            return 1;
        }
        return 0;
    }

    @Override
    public Job getJobById(UUID id) {
        MarklogicJob document = marklogicWrapper.loadDocumentIfPresent(
                StorageProviderUtils.Jobs.NAME,
                id.toString(),
                MarklogicJob.class);
        if (document != null) {
            return document.toJob(jobMapper);
        }
        throw new JobNotFoundException(id);
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        try (JobListVersioner jobListVersioner = new JobListVersioner(jobs)) {
            Transaction transaction = databaseClient.openTransaction();
            if (jobListVersioner.areNewJobs()) {
                marklogicWrapper.putJobs(jobs, jobMapper, transaction);
            } else {
                List<Job> concurrentlyModifiedJobs = new ArrayList<>();
                for (Job job: jobs) {
                    try {
                        marklogicWrapper.patchJob(job, jobMapper, transaction);
                    } catch (ConcurrentJobModificationException e) {
                        concurrentlyModifiedJobs.add(job);
                    }
                }
                if (concurrentlyModifiedJobs.size() > 0) {
                    jobListVersioner.rollbackVersions(concurrentlyModifiedJobs);
                    throw new ConcurrentJobModificationException(jobs);
                }
            }
            transaction.commit();
            jobListVersioner.commitVersions();
        }
        catch (MarkLogicIOException | MarkLogicServerException | MarkLogicBindingException | MarkLogicInternalException e) {
            throw new StorageException(e);
        }
        notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Jobs.NAME + "/"),
                queryBuilder.value(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_STATE),
                        state.name()),
                queryBuilder.range(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_UPDATED_AT),
                        "long",
                        StructuredQueryBuilder.Operator.LT,
                        "" + updatedBefore.toEpochMilli())
        );
        return findJobs(query, pageRequest);
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Jobs.NAME + "/"),
                queryBuilder.value(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_STATE),
                        StateName.SCHEDULED.name()),
                queryBuilder.range(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT),
                        "long",
                        StructuredQueryBuilder.Operator.LT,
                        "" + scheduledBefore.toEpochMilli())
        );
        return findJobs(query, pageRequest);
    }

    public Long countJobs(StateName state) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Jobs.NAME + "/"),
                queryBuilder.value(queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_STATE), state.name())
        );
        return ((Number)marklogicWrapper.countDocuments(query)).longValue();
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Jobs.NAME + "/"),
                queryBuilder.value(queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_STATE), state.name())
        );
        return findJobs(query, pageRequest);
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Jobs.NAME + "/"),
                queryBuilder.value(queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_STATE), state.name())
        );
        return getJobPage(query, pageRequest);
    }

    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Jobs.NAME + "/"),
                queryBuilder.value(queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_STATE), state.name()),
                queryBuilder.range(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_CREATED_AT),
                        "long",
                        StructuredQueryBuilder.Operator.LT,
                        "" + updatedBefore.toEpochMilli())
        );
        List<MarklogicJob> documents =  marklogicWrapper.queryDocuments(query, MarklogicJob.class);
        List<Job> jobsToDelete = documents.stream().map(j -> j.toJob(jobMapper)).collect(Collectors.toList());

        jobsToDelete.forEach(j -> marklogicWrapper.deleteDocument(StorageProviderUtils.Jobs.NAME, j.getId().toString()));
        jobsToDelete.forEach(j -> disposeJobResources(j.getMetadata()));

        final long deletedCount = jobsToDelete.size();
        notifyJobStatsOnChangeListenersIf(deletedCount > 0);
        return (int) deletedCount;
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Jobs.NAME + "/"),
                queryBuilder.value(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_STATE),
                        Arrays
                                .stream(states)
                                .map(Enum::toString)
                                .collect(Collectors.toSet())
                                .toArray(new String[]{}))
                );
        List<MarklogicJob> documents =  marklogicWrapper.queryDocuments(query, MarklogicJob.class);
        return documents
                .stream()
                .map(d -> (String)d.get(StorageProviderUtils.Jobs.FIELD_JOB_SIGNATURE))
                .collect(Collectors.toSet());

    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName... states) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Jobs.NAME + "/"),
                queryBuilder.value(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_STATE),
                        Arrays
                                .stream(states)
                                .map(Enum::toString)
                                .collect(Collectors.toSet())
                                .toArray(new String[]{})),
                queryBuilder.value(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_JOB_SIGNATURE),
                        getJobSignature(jobDetails))
        );
        return marklogicWrapper.countDocuments(query) > 0;
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        StructuredQueryDefinition query = queryBuilder.and(
                queryBuilder.directory(1, "/" + StorageProviderUtils.Jobs.NAME + "/"),
                queryBuilder.value(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_STATE),
                        Arrays
                                .stream(states)
                                .map(Enum::toString)
                                .collect(Collectors.toSet())
                                .toArray(new String[]{})),
                queryBuilder.value(
                        queryBuilder.jsonProperty(StorageProviderUtils.Jobs.FIELD_RECURRING_JOB_ID),
                        recurringJobId)
        );
        return marklogicWrapper.countDocuments(query) > 0;
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        MarklogicRecurringJob jobMarklogicDocument = new MarklogicRecurringJob(jobMapper, recurringJob);
        marklogicWrapper.putDocument(StorageProviderUtils.RecurringJobs.NAME, recurringJob.getId(), jobMarklogicDocument);
        return recurringJob;
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        List<MarklogicRecurringJob> documents = marklogicWrapper.loadAllDocumentsInDirectory(
                StorageProviderUtils.RecurringJobs.NAME,
                MarklogicRecurringJob.class);
        return documents.stream().map(d -> d.toRecurringJob(jobMapper)).collect(Collectors.toList());
    }

    @Override
    public int deleteRecurringJob(String id) {
        if (marklogicWrapper.deleteDocument(StorageProviderUtils.RecurringJobs.NAME, id))
            return 1;
        return 0;
    }

    @Override
    public JobStats getJobStats() {
        Instant instant = Instant.now();
        final MarklogicMetadata succeededJobStats = marklogicWrapper.loadDocumentIfPresent(
                StorageProviderUtils.Metadata.NAME,
                StorageProviderUtils.Metadata.STATS_ID,
                MarklogicMetadata.class);
        final long allTimeSucceededCount = (succeededJobStats != null ? succeededJobStats.getValueAsLong() : 0L);

        Long scheduledCount = countJobs(StateName.SCHEDULED);
        Long enqueuedCount = countJobs(StateName.ENQUEUED);
        Long processingCount = countJobs(StateName.PROCESSING);
        Long succeededCount = countJobs(StateName.SUCCEEDED);
        Long failedCount = countJobs(StateName.FAILED);
        Long deletedCount = countJobs(StateName.DELETED);

        final long total = scheduledCount + enqueuedCount + processingCount + succeededCount + failedCount;
        final int recurringJobCount = (int)marklogicWrapper.countAllDocumentsInDirectory(StorageProviderUtils.RecurringJobs.NAME);
        final int backgroundJobServerCount = (int)marklogicWrapper.countAllDocumentsInDirectory(StorageProviderUtils.BackgroundJobServers.NAME);

        return new JobStats(
                instant,
                total,
                scheduledCount,
                enqueuedCount,
                processingCount,
                failedCount,
                succeededCount,
                allTimeSucceededCount,
                deletedCount,
                recurringJobCount,
                backgroundJobServerCount
        );
    }

    @Override
    public void publishTotalAmountOfSucceededJobs(int amount) {
        Transaction transaction = databaseClient.openTransaction();
        try {
            MarklogicMetadata document = marklogicWrapper.loadDocumentIfPresent(
                    StorageProviderUtils.Metadata.NAME,
                    StorageProviderUtils.Metadata.STATS_ID,
                    MarklogicMetadata.class,
                    transaction);
            if (document != null) {
                DocumentPatchBuilder patchBuilder = databaseClient.newJSONDocumentManager().newPatchBuilder();
                long newAmount = document.getValueAsLong() + amount;
                DocumentPatchHandle patch = patchBuilder
                        .replaceValue("/"+ StorageProviderUtils.Metadata.FIELD_VALUE, newAmount).build();
                marklogicWrapper.patchDocumentIfPresent(
                        StorageProviderUtils.Metadata.NAME,
                        StorageProviderUtils.Metadata.STATS_ID,
                        patch,
                        transaction);
            } else {
                Map<String, Object> stat = new HashMap<>();
                stat.put(StorageProviderUtils.Metadata.FIELD_VALUE, amount);
                marklogicWrapper.putDocument(
                        StorageProviderUtils.Metadata.NAME,
                        StorageProviderUtils.Metadata.STATS_ID,
                        stat,
                        transaction);
            }
        } finally {
            transaction.commit();
        }
    }

    private Page<Job> getJobPage(StructuredQueryDefinition query, PageRequest pageRequest) {
        long count = marklogicWrapper.countDocuments(query);
        if (count > 0) {
            List<Job> jobs = findJobs(query, pageRequest);
            return new Page<>(count, jobs, pageRequest);
        }
        return new Page<>(0, new ArrayList<>(), pageRequest);
    }

    private List<Job> findJobs(StructuredQueryDefinition query, PageRequest pageRequest) {
        List<MarklogicJob> jobs = marklogicWrapper.queryDocuments(query, pageRequest, MarklogicJob.class);
        return jobs.stream().map(j -> j.toJob(jobMapper)).collect(Collectors.toList());
    }

    private void runMigrations() {
        new MarklogicDBCreator(this, marklogicWrapper).runMigrations();
    }
}
