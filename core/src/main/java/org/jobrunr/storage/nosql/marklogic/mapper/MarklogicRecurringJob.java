package org.jobrunr.storage.nosql.marklogic.mapper;


import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProviderUtils;

import java.util.HashMap;

public class MarklogicRecurringJob extends HashMap<String, Object> {

  public MarklogicRecurringJob() {
    super();
  }

  public MarklogicRecurringJob(JobMapper jobMapper, RecurringJob recurringJob) {
    super();

    put(StorageProviderUtils.RecurringJobs.FIELD_ID, recurringJob.getId());
    put(StorageProviderUtils.RecurringJobs.FIELD_VERSION, recurringJob.getVersion());
    put(StorageProviderUtils.RecurringJobs.FIELD_JOB_AS_JSON, jobMapper.serializeRecurringJob(recurringJob));
  }

  public RecurringJob toRecurringJob(JobMapper jobMapper) {
    return jobMapper.deserializeRecurringJob(get(StorageProviderUtils.RecurringJobs.FIELD_JOB_AS_JSON).toString());
  }
}
