package org.jobrunr.storage.nosql.marklogic.migrations;

import org.jobrunr.storage.nosql.marklogic.MarklogicWrapper;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.Jobs;

public class M001_CreateJobCollection extends MarklogicMigration {

    @Override
    public void runMigration(MarklogicWrapper marklogicWrapper) throws IOException {
        if (marklogicWrapper.createDirectory(Jobs.NAME)) {
            marklogicWrapper.createRangeIndex("long", Jobs.FIELD_SCHEDULED_AT);
            marklogicWrapper.createRangeIndex("long", Jobs.FIELD_UPDATED_AT);
            marklogicWrapper.createRangeIndex("long", Jobs.FIELD_CREATED_AT);
        }
    }
}
