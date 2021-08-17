package org.jobrunr.storage.nosql.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@Testcontainers
class MarklogicStorageProviderTest extends StorageProviderTest {
    @Container
    private static final GenericContainer marklogicContainer = new GenericContainer("store/marklogicdb/marklogic-server:10.0-6.1-dev-centos")
            .withExposedPorts(8000, 8001, 8002)
            .withEnv("MARKLOGIC_INIT", "true")
            .withEnv("MARKLOGIC_ADMIN_USERNAME", "admin")
            .withEnv("MARKLOGIC_ADMIN_PASSWORD", "admin")
            .waitingFor(new MarklogicWaitStrategy("admin", "admin"));

    @Override
    protected void cleanup() {
        DatabaseClient databaseClient = marklogicClient();
        MarklogicWrapper marklogicWrapper = new MarklogicWrapper(databaseClient, new JacksonJsonMapper());
        try {
            marklogicWrapper.deleteDirectory(StorageProviderUtils.Jobs.NAME);
            marklogicWrapper.deleteDirectory(StorageProviderUtils.RecurringJobs.NAME);
            marklogicWrapper.deleteDirectory(StorageProviderUtils.BackgroundJobServers.NAME);
            marklogicWrapper.deleteDirectory(StorageProviderUtils.Metadata.NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final MarklogicStorageProvider marklogicStorageProvider = new MarklogicStorageProvider(marklogicClient(), rateLimit().withoutLimits());
        marklogicStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return marklogicStorageProvider;

    }

    private DatabaseClient marklogicClient() {
        return DatabaseClientFactory.newClient(
            marklogicContainer.getContainerIpAddress(),
            marklogicContainer.getMappedPort(8000),
            new DatabaseClientFactory.DigestAuthContext("admin", "admin"));
    }
}