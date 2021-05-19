package org.jobrunr.storage.nosql.marklogic.migrations;

import org.jobrunr.storage.nosql.marklogic.MarklogicWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.jobrunr.storage.StorageProviderUtils.Metadata;

public class M005_CreateMetadataCollectionAndDropJobStats extends MarklogicMigration {

    @Override
    public void runMigration(MarklogicWrapper marklogicWrapper) throws IOException {
        if (marklogicWrapper.createDirectory(Metadata.NAME)) {
            final Map<String, Object> document = new HashMap<String, Object>();
            document.put(Metadata.FIELD_ID, Metadata.STATS_ID);
            document.put(Metadata.FIELD_NAME, "succeeded-jobs-counter");
            document.put(Metadata.FIELD_OWNER, "cluster");
            document.put(Metadata.FIELD_VALUE, 0L);
            marklogicWrapper.putDocument(Metadata.NAME, Metadata.STATS_ID, document);
        }
    }
}
