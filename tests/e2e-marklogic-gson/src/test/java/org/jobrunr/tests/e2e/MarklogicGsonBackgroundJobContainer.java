package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.marklogic.MarklogicStorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class MarklogicGsonBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final GenericContainer marklogicContainer;
    private final Network network;

    public MarklogicGsonBackgroundJobContainer(GenericContainer marklogicContainer, Network network) {
        super("jobrunr-e2e-marklogic-gson:1.0");
        this.marklogicContainer = marklogicContainer;
        this.network = network;
    }

    @Override
    public void start() {
        this
                .dependsOn(marklogicContainer)
                .withNetwork(network)
                .withEnv("MARKLOGIC_HOST", "marklogic")
                .withEnv("MARKLOGIC_PORT", String.valueOf(8000))
                .withEnv("MARKLOGIC_USERNAME", "admin")
                .withEnv("MARKLOGIC_PASSWORD", "admin");

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        return new MarklogicStorageProvider(marklogicContainer.getContainerIpAddress(), marklogicContainer.getFirstMappedPort(), "admin", "admin");
    }
}
