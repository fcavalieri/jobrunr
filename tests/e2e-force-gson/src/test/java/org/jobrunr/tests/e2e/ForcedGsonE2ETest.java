package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ForcedGsonE2ETest extends AbstractE2EForcedGsonTest {

    private static final Network network = Network.newNetwork();

    @Container
    private static final GenericContainer mongoContainer = new GenericContainer("mongo:4.2.8")
            .withNetwork(network)
            .withNetworkAliases("mongo")
            .withExposedPorts(27017);

    @Container
    private static final ForcedGsonBackgroundJobContainer backgroundJobServer = new ForcedGsonBackgroundJobContainer(mongoContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
