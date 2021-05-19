package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MarklogicJacksonE2ETest extends AbstractE2EJacksonTest {

    private static final Network network = Network.newNetwork();

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
    private static final MarklogicJacksonBackgroundJobContainer backgroundJobServer = new MarklogicJacksonBackgroundJobContainer(marklogicContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
