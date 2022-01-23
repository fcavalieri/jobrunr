package org.jobrunr.testcontainers;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.admin.QueryOptionsManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class MarklogicWaitStrategy extends AbstractWaitStrategy {
  private String username;
  private String password;

  public MarklogicWaitStrategy(String username, String password) {
    this.username = username;
    this.password = password;
    this.startupTimeout = Duration.ofSeconds(120);
  }

  @Override
  protected void waitUntilReady() {
    final int marklogicMainPort = waitStrategyTarget.getMappedPort(8000);
    final int marklogicLogPort = waitStrategyTarget.getMappedPort(9000);

    URI marklogicLogUri;
    try {
      marklogicLogUri = new URI("http://" + waitStrategyTarget.getHost() + ":" + marklogicLogPort);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(marklogicLogUri).GET().build();
      HttpClient httpClient = HttpClient.newHttpClient();
      Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
        getRateLimiter().doWhenReady(() -> {
          try {
            System.out.println("Connecting to log port...");
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200)
              throw new RuntimeException("Status is " + response.statusCode());
          } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
          }
        });
        return true;
      });

    } catch (TimeoutException e) {
      throw new ContainerLaunchException("Timed out waiting for Marklogic to be accessible");
    }
    System.out.println("Marklogic log connection valid");

    DatabaseClient databaseClient = DatabaseClientFactory.newClient(waitStrategyTarget.getHost(), marklogicMainPort,
            new DatabaseClientFactory.DigestAuthContext(username, password));

    try {
      Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
        getRateLimiter().doWhenReady(() -> {
          System.out.println("Checking database client connectivity...");
          DatabaseClient.ConnectionResult connectionResult = databaseClient.checkConnection();
          if (!connectionResult.isConnected()) {
            throw new RuntimeException();
          }
        });
        return true;
      });

    } catch (TimeoutException e) {
      throw new ContainerLaunchException("Timed out waiting for Marklogic to be accessible");
    }
    System.out.println("Connection valid");

    try {
      Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
        getRateLimiter().doWhenReady(() -> {
          System.out.println("Checking database querying...");
          QueryManager queryManager = databaseClient.newQueryManager();
          StructuredQueryBuilder sqb = queryManager.newStructuredQueryBuilder();
          StructuredQueryDefinition sqd = sqb.directory(1, "/test/");
          SearchHandle results = queryManager.search(sqd, new SearchHandle());

          String jsonOptions = "{\"options\":{\"return-results\": \"true\", \"transform-results\": { \"apply\": \"raw\" }}}";
          QueryOptionsManager optionsMgr = databaseClient.newServerConfigManager().newQueryOptionsManager();
          optionsMgr.writeOptions("test", new StringHandle(jsonOptions).withFormat(Format.JSON));
        });
        return true;
      });

    } catch (TimeoutException e) {
      throw new ContainerLaunchException("Timed out waiting for Marklogic to be accessible");
    }
    System.out.println("Connection accepts commands. Marklogic has started.");
    System.out.println(waitStrategyTarget.getLogs());
  }
}
