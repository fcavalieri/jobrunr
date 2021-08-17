package org.jobrunr.tests.e2e;

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

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class MarklogicWaitStrategy extends AbstractWaitStrategy {
  private String username;
  private String password;

  public MarklogicWaitStrategy(String username, String password) {
    this.username = username;
    this.password = password;
  }

  private URI buildLivenessUri(int livenessCheckPort) {
    final String scheme = "http" + "://";
    final String host = waitStrategyTarget.getHost();

    final String portSuffix = ":" + livenessCheckPort;

    return URI.create(scheme + host + portSuffix + "/");
  }

  @Override
  protected void waitUntilReady() {
    final String containerName = waitStrategyTarget.getContainerInfo().getName();
    final int livenessCheckPort = waitStrategyTarget.getMappedPort(8000);

    final URI rawUri = buildLivenessUri(livenessCheckPort);
    final String uri = rawUri.toString();

    try {
      // Un-map the port for logging
      int originalPort = waitStrategyTarget.getExposedPorts().stream()
              .filter(exposedPort -> rawUri.getPort() == waitStrategyTarget.getMappedPort(exposedPort))
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Target port " + rawUri.getPort() + " is not exposed"));
      System.out.println(String.format("%s: Waiting for %d seconds for URL: %s (where port %d maps to container port %d)", containerName, startupTimeout.getSeconds(), uri, rawUri.getPort(), originalPort));
    } catch (RuntimeException e) {
      // do not allow a failure in logging to prevent progress, but log for diagnosis
      System.out.println("Unexpected error occurred - will proceed to try to wait anyway " + e);
    }

    DatabaseClient databaseClient = DatabaseClientFactory.newClient(
              waitStrategyTarget.getHost(), livenessCheckPort, new DatabaseClientFactory.DigestAuthContext(username, password));

    try {
      Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
        getRateLimiter().doWhenReady(() -> {
          DatabaseClient.ConnectionResult connectionResult = databaseClient.checkConnection();
          if (!connectionResult.isConnected()) {
            System.out.println(String.format("Continuing to wait (%d): %s", connectionResult.getStatusCode(), connectionResult.getErrorMessage()));
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
    System.out.println("Connection accepts commands");

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("Waited 10 extra seconds");
  }
}



