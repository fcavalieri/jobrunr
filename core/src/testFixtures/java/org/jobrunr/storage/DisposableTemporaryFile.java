package org.jobrunr.storage;

import org.jobrunr.jobs.metadata.DisposableResource;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

  public class DisposableTemporaryFile implements DisposableResource, Serializable {
    private File disposableFile;

    public DisposableTemporaryFile() {
      try {
        disposableFile = File.createTempFile("jobrunr", "gc");
        disposableFile.deleteOnExit();
      } catch (IOException e) {
        throw new IllegalStateException("Unexpected failure creating garbage collecting test file", e);
      }
    }


    @Override
    public void dispose() throws Exception {
      if (!disposableFile.delete())
        throw new IllegalStateException("Unexpected failure garbage collecting test file");
    }

    public boolean exists() {
      return disposableFile.exists();
    }
  }
