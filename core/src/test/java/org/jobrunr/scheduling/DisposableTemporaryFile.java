package org.jobrunr.scheduling;

import org.jobrunr.jobs.metadata.DisposableResource;

import java.io.File;
import java.io.IOException;

class DisposableTemporaryFile implements DisposableResource {
  private final File disposableFile;
  private final boolean failOnDispose;

  public DisposableTemporaryFile() {
    disposableFile = null;
    failOnDispose = false;
  }

  public DisposableTemporaryFile(File disposableFile, boolean failOnDispose) {
    this.disposableFile = disposableFile;
    this.failOnDispose = failOnDispose;
  }


  @Override
  public void dispose() throws Exception {
    if (disposableFile == null)
      throw new IllegalStateException("Not initialized");
    if (!disposableFile.delete())
      throw new IllegalStateException("Unexpected failure garbage collecting test file");
    if (failOnDispose)
      throw new IllegalStateException("Unit Test Failure");
  }

  public boolean exists() {
    if (disposableFile == null)
      return false;
    return disposableFile.exists();
  }
}
