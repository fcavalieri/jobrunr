package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MarklogicGsonE2ETest extends AbstractE2EForcedGsonTest {

    private static final Network network = Network.newNetwork();

    @Override
    public JobRunrConfiguration.JsonMapperKind overrideJsonMapperKind() {
        return JobRunrConfiguration.JsonMapperKind.GSON;
    }

    @Container
    private static final GenericContainer marklogicContainer = new GenericContainer("store/marklogicdb/marklogic-server:10.0-6.1-dev-centos")
            .withNetwork(network)
            .withNetworkAliases("marklogic")
            .withExposedPorts(8000, 8001, 8002)
            .withEnv("MARKLOGIC_INIT", "true")
            .withEnv("MARKLOGIC_ADMIN_USERNAME", "admin")
            .withEnv("MARKLOGIC_ADMIN_PASSWORD", "admin")
            .waitingFor(new MarklogicWaitStrategy("admin", "admin"));

    @Container
    private static final MarklogicGsonBackgroundJobContainer backgroundJobServer = new MarklogicGsonBackgroundJobContainer(marklogicContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
