package org.jobrunr.storage.nosql.marklogic.migrations;

import org.jobrunr.storage.nosql.marklogic.MarklogicWrapper;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

public class M002_CreateRecurringJobCollection extends MarklogicMigration {

    @Override
    public void runMigration(MarklogicWrapper marklogicWrapper) throws IOException {
        marklogicWrapper.createDirectory(RecurringJobs.NAME);
    }
}
