package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.marklogic.MarklogicStorageProvider;

public class Main extends AbstractMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected StorageProvider initStorageProvider() {
        if (getEnvOrProperty("MARKLOGIC_HOST") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable MARKLOGIC_HOST is not set");
        }
        if (getEnvOrProperty("MARKLOGIC_PORT") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable MARKLOGIC_PORT is not set");
        }
        if (getEnvOrProperty("MARKLOGIC_USERNAME") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable MARKLOGIC_USERNAME is not set");
        }
        if (getEnvOrProperty("MARKLOGIC_PASSWORD") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable MARKLOGIC_PASSWORD is not set");
        }

        return new MarklogicStorageProvider(getEnvOrProperty("MARKLOGIC_HOST"),
                Integer.parseInt(getEnvOrProperty("MARKLOGIC_PORT")),
                getEnvOrProperty("MARKLOGIC_USERNAME"),
                getEnvOrProperty("MARKLOGIC_PASSWORD")
        );
    }
}
