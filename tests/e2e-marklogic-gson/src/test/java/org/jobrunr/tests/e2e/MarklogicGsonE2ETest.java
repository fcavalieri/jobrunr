package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.testcontainers.MarklogicWaitStrategy;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.jobrunr.utils.Constants.MARKLOGIC_IMAGE;

@Testcontainers
public class MarklogicGsonE2ETest extends AbstractE2EForcedGsonTest {

    private static final Network network = Network.newNetwork();

    @Override
    public JobRunrConfiguration.JsonMapperKind overrideJsonMapperKind() {
        return JobRunrConfiguration.JsonMapperKind.GSON;
    }

    @Container
    private static final GenericContainer marklogicContainer = new GenericContainer(MARKLOGIC_IMAGE)
            .withNetwork(network)
            .withNetworkAliases("marklogic")
            .withExposedPorts(8000, 8001, 8002, 9000)
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
