package org.jobrunr.storage.nosql.marklogic.migrations;

import org.jobrunr.storage.nosql.marklogic.MarklogicWrapper;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;

public class M003_CreateBackgroundJobServerCollection extends MarklogicMigration {

    @Override
    public void runMigration(MarklogicWrapper marklogicWrapper) throws IOException {
        if (marklogicWrapper.createDirectory(BackgroundJobServers.NAME)) {
            marklogicWrapper.createRangeIndex("long", BackgroundJobServers.FIELD_LAST_HEARTBEAT);
        }

    }
}
