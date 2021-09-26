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
    private final HttpClient httpClient;

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
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Connection");
        this.baseUri = baseUri;
        try
        {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            httpClient = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .sslContext(sslContext)
                        .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

    public HttpResponse<String> get(String url) {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + url))
                .header("Connection", "close")
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }

    public HttpResponse<String> get(String url, Object... params) {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + String.format(url, params)))
                .header("Connection", "close")
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }

    public HttpResponse<String> delete(String url, Object... params) {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + String.format(url, params)))
                .header("Connection", "close")
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
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + String.format(url, params)))
                .header("Connection", "close")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }
}
