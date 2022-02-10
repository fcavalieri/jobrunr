package org.jobrunr.storage.nosql.marklogic.mapper;

import com.marklogic.client.document.DocumentPatchBuilder;
import com.marklogic.client.io.marker.DocumentPatchHandle;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProviderUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class MarklogicBackgroundJobServerStatus extends HashMap<String, Object> {
  public MarklogicBackgroundJobServerStatus() {
    super();
  }

  public MarklogicBackgroundJobServerStatus(BackgroundJobServerStatus serverStatus) {
    super();

    put(StorageProviderUtils.BackgroundJobServers.FIELD_ID, serverStatus.getId().toString());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_WORKER_POOL_SIZE, serverStatus.getWorkerPoolSize());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS, serverStatus.getPollIntervalInSeconds());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER, serverStatus.getDeleteSucceededJobsAfter().toString());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_FAILED_JOBS_AFTER, serverStatus.getDeleteFailedJobsAfter().toString());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER, serverStatus.getPermanentlyDeleteDeletedJobsAfter().toString());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT, serverStatus.getFirstHeartbeat().toEpochMilli());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat().toEpochMilli());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_IS_RUNNING, serverStatus.isRunning());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY, serverStatus.getSystemTotalMemory());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY, serverStatus.getProcessMaxMemory());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory());
    put(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad());
  }

  public static DocumentPatchHandle toUpdateDocument(BackgroundJobServerStatus serverStatus, DocumentPatchBuilder dpb) {
    return dpb.replaceValue("/" + StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat().toEpochMilli())
            .replaceValue("/" + StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory())
            .replaceValue("/" + StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad())
            .replaceValue("/" + StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory())
            .replaceValue("/" + StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory())
            .replaceValue("/" + StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad()).build();
  }

  public BackgroundJobServerStatus toBackgroundJobServerStatus() {
    return new BackgroundJobServerStatus(
            UUID.fromString((String)get(StorageProviderUtils.BackgroundJobServers.FIELD_ID)),
            ((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_WORKER_POOL_SIZE)).intValue(),
            ((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS)).intValue(),
            Duration.parse((String)get(StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER)),
            Duration.parse((String)get(StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_FAILED_JOBS_AFTER)),
            Duration.parse((String)get(StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER)),
            Instant.ofEpochMilli(((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT)).longValue()),
            Instant.ofEpochMilli(((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT)).longValue()),
            (boolean)get(StorageProviderUtils.BackgroundJobServers.FIELD_IS_RUNNING),
            ((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY)).longValue(),
            ((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY)).longValue(),
            ((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD)).doubleValue(),
            ((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY)).longValue(),
            ((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY)).longValue(),
            ((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY)).longValue(),
            ((Number)get(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_CPU_LOAD)).doubleValue()
    );
  }

  public boolean isRunning() {
    return (boolean)get(StorageProviderUtils.BackgroundJobServers.FIELD_IS_RUNNING);
  }
}
