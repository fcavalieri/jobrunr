package org.jobrunr.jobs.metadata;

import java.io.Serializable;

public interface DisposableResource extends Serializable {
  void dispose() throws Exception;
}
