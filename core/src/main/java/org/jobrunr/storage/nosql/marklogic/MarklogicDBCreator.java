package org.jobrunr.storage.nosql.marklogic;

import org.jobrunr.storage.nosql.common.NoSqlDatabaseCreator;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.marklogic.migrations.MarklogicMigration;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.jobrunr.storage.StorageProviderUtils.Migrations;


public class MarklogicDBCreator extends NoSqlDatabaseCreator<MarklogicMigration> {

    private final MarklogicWrapper marklogicWrapper;

    public MarklogicDBCreator(MarklogicStorageProvider marklogicStorageProvider, MarklogicWrapper marklogicWrapper) {
        super(marklogicStorageProvider);
        this.marklogicWrapper = marklogicWrapper;
    }

    @Override
    protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
        return !marklogicWrapper.existsDocument(Migrations.NAME, noSqlMigration.getClassName());
    }

    @Override
    protected void runMigration(MarklogicMigration noSqlMigration) throws IOException {
        noSqlMigration.runMigration(marklogicWrapper);
    }

    @Override
    protected boolean markMigrationAsDone(NoSqlMigration noSqlMigration) {
        Map<String, Object> document = new HashMap<String, Object>();
        document.put(Migrations.FIELD_ID, noSqlMigration.getClassName());
        document.put(Migrations.FIELD_NAME, noSqlMigration.getClassName());
        document.put(Migrations.FIELD_DATE, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        marklogicWrapper.putDocument(Migrations.NAME, noSqlMigration.getClassName(), document);
        return true;
    }
}
