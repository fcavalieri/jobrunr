package org.jobrunr.storage.nosql.marklogic.mapper;

import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProviderUtils;

import java.time.Instant;
import java.util.HashMap;

public class MarklogicMetadata extends HashMap<String, Object> {
  public MarklogicMetadata() {
    super();
  }

  public MarklogicMetadata(JobRunrMetadata metadata) {
    super();

    put(StorageProviderUtils.Metadata.FIELD_ID, metadata.getId());
    put(StorageProviderUtils.Metadata.FIELD_NAME, metadata.getName());
    put(StorageProviderUtils.Metadata.FIELD_OWNER, metadata.getOwner());
    put(StorageProviderUtils.Metadata.FIELD_VALUE, metadata.getValue());
    put(StorageProviderUtils.Metadata.FIELD_CREATED_AT, metadata.getCreatedAt().toEpochMilli());
    put(StorageProviderUtils.Metadata.FIELD_UPDATED_AT, metadata.getUpdatedAt().toEpochMilli());
  }

  public JobRunrMetadata toJobRunrMetadata() {
    return new JobRunrMetadata(
            (String)get(StorageProviderUtils.Metadata.FIELD_NAME),
            (String)get(StorageProviderUtils.Metadata.FIELD_OWNER),
            (String)get(StorageProviderUtils.Metadata.FIELD_VALUE),
            Instant.ofEpochMilli(((Number)get(StorageProviderUtils.Metadata.FIELD_CREATED_AT)).longValue()),
            Instant.ofEpochMilli(((Number)get(StorageProviderUtils.Metadata.FIELD_UPDATED_AT)).longValue())
    );
  }

  /*
   * Job statistics metadata does not conform to the above schema.
   */
  public long getValueAsLong() {
    return ((Number)get(StorageProviderUtils.Metadata.FIELD_VALUE)).longValue();
  }
}

