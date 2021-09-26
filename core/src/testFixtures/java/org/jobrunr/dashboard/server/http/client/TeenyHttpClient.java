package org.jobrunr.dashboard.server.http.client;

import org.jobrunr.utils.exceptions.Exceptions;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class TeenyHttpClient {

    private final String baseUri;

    private static TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
    };

    public TeenyHttpClient(String baseUri) {
        this.baseUri = baseUri;
    }

    /*
     * When using http 1.1 persistent connections, sometimes the server closes the connection after a while.
     * If we reuse the same http client we get sometimes exceptions like:
     *  java.lang.RuntimeException: java.io.IOException: HTTP/1.1 header parser received no bytes
        at org.jobrunr.dashboard.server.http.client.TeenyHttpClient.unchecked(TeenyHttpClient.java:81)
        at org.jobrunr.dashboard.server.http.client.TeenyHttpClient.post(TeenyHttpClient.java:91)
     * Either we send Connection: close (before java 12 we cannot) or we do not reuse the http client in tests.
     */
    private HttpClient getHttpClient() {
        try
        {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .sslContext(sslContext)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

    public HttpResponse<String> get(String url) {
        HttpClient httpClient = getHttpClient();
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + url))
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }

    public HttpResponse<String> get(String url, Object... params) {
        HttpClient httpClient = getHttpClient();
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + String.format(url, params)))
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }

    public HttpResponse<String> delete(String url, Object... params) {
        HttpClient httpClient = getHttpClient();
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + String.format(url, params)))
                .DELETE()
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }

    private <T> T unchecked(Exceptions.ThrowingSupplier<T> throwingSupplier) {
        try {
            return throwingSupplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse<String> post(String url, Object... params) {
        HttpClient httpClient = getHttpClient();
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + String.format(url, params)))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }
}
