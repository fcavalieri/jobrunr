package org.jobrunr.jobs.metadata;

import org.jobrunr.jobs.context.JobContext;

import java.io.Serializable;

public interface DisposableResource extends Serializable, JobContext.Metadata {
  void dispose() throws Exception;
}
