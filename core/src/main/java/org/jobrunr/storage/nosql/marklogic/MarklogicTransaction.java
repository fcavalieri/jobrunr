package org.jobrunr.storage.nosql.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.Transaction;
import org.jobrunr.jobs.Job;

public class MarklogicTransaction implements AutoCloseable {

  private Transaction transaction = null;
  private boolean isVersionCommitted = false;


  public MarklogicTransaction(DatabaseClient databaseClient) {
    this.transaction = databaseClient.openTransaction();
  }

  public void commit() {
    transaction.commit();
    isVersionCommitted = true;
  }

  Transaction getTransaction() {
    return transaction;
  }

  @Override
  public void close() {
    if (!isVersionCommitted) {
      transaction.rollback();
    }
  }
}
