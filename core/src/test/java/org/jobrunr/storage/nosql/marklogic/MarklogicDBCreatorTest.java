package org.jobrunr.storage.nosql.marklogic;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.admin.QueryOptionsManager;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.apache.http.HttpHost;
import org.bson.Document;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.h2.engine.Database;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationByClass;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchDBCreator;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.jobrunr.storage.nosql.marklogic.migrations.M001_CreateJobCollection;
import org.jobrunr.storage.nosql.marklogic.migrations.M002_CreateRecurringJobCollection;
import org.jobrunr.storage.nosql.redis.JedisRedisDBCreator;
import org.jobrunr.storage.nosql.redis.JedisRedisStorageProvider;
import org.jobrunr.storage.nosql.redis.migrations.M001_JedisRemoveJobStatsAndUseMetadata;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.JedisPool;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class MarklogicDBCreatorTest {

    @Container
    private static final GenericContainer marklogicContainer = new GenericContainer("store/marklogicdb/marklogic-server:10.0-6.1-dev-centos")
            .withExposedPorts(8000, 8001, 8002)
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
