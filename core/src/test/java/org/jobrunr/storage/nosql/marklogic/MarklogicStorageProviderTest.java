package org.jobrunr.storage.nosql.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.MarkLogicIOException;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.testcontainers.MarklogicWaitStrategy;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.jobrunr.utils.Constants.MARKLOGIC_IMAGE;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@Testcontainers
class MarklogicStorageProviderTest extends StorageProviderTest {
    @Container
    private static final GenericContainer marklogicContainer = new GenericContainer(MARKLOGIC_IMAGE)
            .withExposedPorts(8000, 8001, 8002, 9000)
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

    @Override
    protected ThrowingStorageProvider makeThrowingStorageProvider(StorageProvider storageProvider) {
        return new ThrowingMarklogicStorageProvider(storageProvider);
    }

    private DatabaseClient marklogicClient() {
        return DatabaseClientFactory.newClient(
            marklogicContainer.getContainerIpAddress(),
            marklogicContainer.getMappedPort(8000),
            new DatabaseClientFactory.DigestAuthContext("admin", "admin"));
    }

    protected static class ThrowingMarklogicStorageProvider extends ThrowingStorageProvider {

        public ThrowingMarklogicStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "marklogicWrapper");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider storageProvider) {
            MarklogicWrapper mockedMarklogicWrapper = mock(MarklogicWrapper.class);
            MarkLogicIOException markLogicIOException = mock(MarkLogicIOException.class);
            doThrow(markLogicIOException).when(mockedMarklogicWrapper).putJob(any(), any(), any());
            doThrow(markLogicIOException).when(mockedMarklogicWrapper).putJobs(any(), any(), any());
            doThrow(markLogicIOException).when(mockedMarklogicWrapper).patchJob(any(), any(), any());
            setInternalState(storageProvider, "marklogicWrapper", mockedMarklogicWrapper);
        }
    }
}