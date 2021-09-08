package org.jobrunr.dashboard.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractWebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebServer.class);

    protected HttpServer httpServer;
    protected ExecutorService executorService;
    protected Set<HttpExchangeHandler> httpHandlers;

    public HttpContext createContext(HttpExchangeHandler httpHandler) {
        httpHandlers.add(httpHandler);
        return httpServer.createContext(httpHandler.getContextPath(), httpHandler);
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpHandlers.forEach(this::closeHttpHandler);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        httpServer.stop(0);
    }

    public String getWebServerProtocol() {
        if (httpServer instanceof HttpsServer)
            return "https";
        return "http";
    }

    public String getWebServerHostAddress() {
        if (httpServer.getAddress().getAddress().isAnyLocalAddress()) {
            return "localhost";
        }
        return httpServer.getAddress().getAddress().getHostAddress();
    }

    public int getWebServerHostPort() {
        return httpServer.getAddress().getPort();
    }

    private void closeHttpHandler(HttpExchangeHandler httpHandler) {
        try {
            httpHandler.close();
        } catch (Exception shouldNotHappen) {
            LOGGER.warn("Error closing HttpHandler", shouldNotHappen);
        }
    }
}
