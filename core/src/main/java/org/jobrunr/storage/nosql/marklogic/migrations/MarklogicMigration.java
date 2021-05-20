package org.jobrunr.storage.nosql.marklogic.migrations;

import org.jobrunr.storage.nosql.marklogic.MarklogicWrapper;

import java.io.IOException;

public abstract class MarklogicMigration {

    public abstract void runMigration(MarklogicWrapper marklogicWrapper) throws IOException;
}
