package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Arrays;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@Testcontainers
public class MongoDB3StorageProviderTest extends StorageProviderTest {

    @Container
    private static final GenericContainer mongoContainer = new GenericContainer("mongo:3.4").withExposedPorts(27017);

    private static MongoClient mongoClient;

    @Override
    protected void cleanup() {
        MongoDatabase jobrunrDb = mongoClient().getDatabase(MongoDBStorageProvider.DEFAULT_DB_NAME);

        jobrunrDb
                .listCollectionNames()
                .into(new ArrayList<>()).stream()
                .filter(collectionName -> !collectionName.equals(StorageProviderUtils.Migrations.NAME))
                .forEach(collectionName -> jobrunrDb.getCollection(collectionName).deleteMany(new Document()));
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final MongoDBStorageProvider dbStorageProvider = new MongoDBStorageProvider(mongoClient(), rateLimit().withoutLimits());
        dbStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return dbStorageProvider;
    }

    @AfterAll
    public static void closeMongoClient() {
        mongoClient.close();
    }

    private MongoClient mongoClient() {
        if (mongoClient == null) {
            CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                    CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
                    MongoClientSettings.getDefaultCodecRegistry()
            );
            mongoClient = MongoClients.create(
                    MongoClientSettings.builder()
                            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoContainer.getContainerIpAddress(), mongoContainer.getMappedPort(27017)))))
                            .codecRegistry(codecRegistry)
                            .build());

        }
        return mongoClient;
    }
}
