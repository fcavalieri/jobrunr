package org.jobrunr.storage.nosql.marklogic.mapper;

import com.marklogic.client.document.DocumentPatchBuilder;
import com.marklogic.client.io.marker.DocumentPatchHandle;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProviderUtils;

import java.util.HashMap;

public class MarklogicJob extends HashMap<String, Object> {
  public MarklogicJob() {
    super();
  }

  public MarklogicJob(JobMapper jobMapper, Job job) {
    super();

    put(StorageProviderUtils.Jobs.FIELD_ID, job.getId().toString());
    put(StorageProviderUtils.Jobs.FIELD_VERSION, job.increaseVersion());
    put(StorageProviderUtils.Jobs.FIELD_JOB_AS_JSON, jobMapper.serializeJob(job));
    put(StorageProviderUtils.Jobs.FIELD_JOB_SIGNATURE, job.getJobSignature());
    put(StorageProviderUtils.Jobs.FIELD_STATE, job.getState().name());
    put(StorageProviderUtils.Jobs.FIELD_CREATED_AT, job.getCreatedAt().toEpochMilli());
    put(StorageProviderUtils.Jobs.FIELD_UPDATED_AT, job.getUpdatedAt().toEpochMilli());
    if (job.hasState(StateName.SCHEDULED)) {
      put(StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT, job.<ScheduledState>getJobState().getScheduledAt().toEpochMilli());
      put(StorageProviderUtils.Jobs.FIELD_RECURRING_JOB_ID, job.<ScheduledState>getJobState().getRecurringJobId());
    } else {
      put(StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT, null);
      put(StorageProviderUtils.Jobs.FIELD_RECURRING_JOB_ID, null);
    }
  }

  public static DocumentPatchHandle toUpdateDocument(Job job, JobMapper jobMapper, DocumentPatchBuilder dpb) {
    dpb.replaceValue("/" + StorageProviderUtils.Jobs.FIELD_VERSION, job.increaseVersion())
            .replaceValue("/" + StorageProviderUtils.Jobs.FIELD_JOB_AS_JSON, jobMapper.serializeJob(job))
            .replaceValue("/" + StorageProviderUtils.Jobs.FIELD_STATE, job.getState().name())
            .replaceValue("/" + StorageProviderUtils.Jobs.FIELD_UPDATED_AT, job.getUpdatedAt().toEpochMilli());
    if (job.hasState(StateName.SCHEDULED)) {
      dpb.replaceValue("/" + StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT, job.<ScheduledState>getJobState().getScheduledAt().toEpochMilli());
      dpb.replaceValue("/" + StorageProviderUtils.Jobs.FIELD_RECURRING_JOB_ID, job.<ScheduledState>getJobState().getRecurringJobId());
    }
    return dpb.build();
  }

  public Job toJob(JobMapper jobMapper) {
    return jobMapper.deserializeJob(get(StorageProviderUtils.Jobs.FIELD_JOB_AS_JSON).toString());
  }
}
