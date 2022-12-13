package org.jobrunr.dashboard.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.concurrent.Executors;

public class WebServerHttp extends AbstractWebServer {

    public WebServerHttp(int port) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            executorService = Executors.newCachedThreadPool();
            httpServer.setExecutor(executorService);
            httpHandlers = new HashSet<>();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getWebServerProtocol() {
        if (httpServer instanceof HttpsServer)
            return "https";
        return "http";
    }
}
