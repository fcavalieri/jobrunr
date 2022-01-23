package org.jobrunr.storage.nosql.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationByClass;
import org.jobrunr.storage.nosql.marklogic.migrations.M001_CreateJobCollection;
import org.jobrunr.storage.nosql.marklogic.migrations.M002_CreateRecurringJobCollection;
import org.jobrunr.testcontainers.MarklogicWaitStrategy;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.utils.Constants.MARKLOGIC_IMAGE;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class MarklogicDBCreatorTest {

    @Container
    private static final GenericContainer marklogicContainer = new GenericContainer(MARKLOGIC_IMAGE)
            .withExposedPorts(8000, 8001, 8002, 9000)
            .withEnv("MARKLOGIC_INIT", "true")
            .withEnv("MARKLOGIC_ADMIN_USERNAME", "admin")
            .withEnv("MARKLOGIC_ADMIN_PASSWORD", "admin")
            .waitingFor(new MarklogicWaitStrategy("admin", "admin"));

    @Mock
    private MarklogicStorageProvider marklogicStorageProviderMock;

    @Test
    public void testMigrations() {
        MarklogicDBCreator marklogicDBCreator = new MarklogicDBCreator(marklogicStorageProviderMock, new MarklogicWrapper(marklogicClient(), new JacksonJsonMapper()));

        assertThat(marklogicDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_CreateJobCollection.class))).isTrue();
        assertThat(marklogicDBCreator.isNewMigration(new NoSqlMigrationByClass(M002_CreateRecurringJobCollection.class))).isTrue();

        assertThatCode(marklogicDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(marklogicDBCreator::runMigrations).doesNotThrowAnyException();

        assertThat(marklogicDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_CreateJobCollection.class))).isFalse();
        assertThat(marklogicDBCreator.isNewMigration(new NoSqlMigrationByClass(M002_CreateRecurringJobCollection.class))).isFalse();
    }

    @Test
    public void testMigrationsConcurrent() {
        MarklogicDBCreator marklogicDBCreator = new MarklogicDBCreator(marklogicStorageProviderMock, new MarklogicWrapper(marklogicClient(), new JacksonJsonMapper())) {
            @Override
            protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
                return true;
            }
        };

        assertThatCode(marklogicDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(marklogicDBCreator::runMigrations).doesNotThrowAnyException();
    }

    private DatabaseClient marklogicClient() {
        return DatabaseClientFactory.newClient(
                marklogicContainer.getContainerIpAddress(),
                marklogicContainer.getMappedPort(8000),
                new DatabaseClientFactory.DigestAuthContext("admin", "admin"));
    }
}
