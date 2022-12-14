package org.jobrunr.dashboard.server;

import com.sun.net.httpserver.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private final HttpServer httpServer;
    private final ExecutorService executorService;
    private final Set<HttpExchangeHandler> httpHandlers;
    //JobRunrPlus: support HTTPS dashboard
    private final boolean https;

    public WebServer(int port) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            executorService = Executors.newCachedThreadPool();
            httpServer.setExecutor(executorService);
            httpHandlers = new HashSet<>();
            https = false;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    //JobRunrPlus: support HTTPS dashboard
    public WebServer(int tlsPort, Path keyStorePath, String keyStorePass) {
        try {
            httpServer = HttpsServer.create(new InetSocketAddress(tlsPort), 0);
            SSLContext sslContext = initializeSslContext(keyStorePath, keyStorePass);
            ((HttpsServer) httpServer).setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    try {
                        SSLContext c = SSLContext.getDefault();
                        SSLEngine engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            executorService = Executors.newCachedThreadPool();
            httpServer.setExecutor(executorService);
            httpHandlers = new HashSet<>();
            https = true;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

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

    //JobRunrPlus: support HTTPS dashboard
    public String getWebServerProtocol() {
        return https ? "https" : "http";
    }

    private SSLContext initializeSslContext(Path keyStorePath, String keyStorePass)  {
        char[] keyStorePassArray = (keyStorePass != null ? keyStorePass : "").toCharArray();
        KeyStore keyStore = keyStorePath == null ? generateSelfSignedCertificate(getWebServerHostAddress())
                : loadCertificate(keyStorePath, keyStorePassArray);
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, keyStorePassArray);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | KeyManagementException e) {
            throw new IllegalStateException("Cannot create SSL context", e);
        }
    }

    private static KeyStore generateSelfSignedCertificate(String commonName) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            X500Name dnName = new X500Name("CN=" + commonName);
            BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());
            String signatureAlgorithm = "SHA256WithRSA";
            ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
                    .build(keyPair.getPrivate());
            Instant startDate = Instant.now();
            Instant endDate = startDate.plus(25 * 365, ChronoUnit.DAYS);
            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    dnName, certSerialNumber, Date.from(startDate), Date.from(endDate), dnName,
                    keyPair.getPublic());
            java.security.cert.Certificate certificate = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(certBuilder.build(contentSigner));
            PrivateKey privateKey = keyPair.getPrivate();

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            java.security.cert.Certificate[] chain = new Certificate[]{certificate};
            keyStore.setKeyEntry("alias", privateKey, new char[]{}, chain);
            return keyStore;
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | NoSuchProviderException |
                 OperatorCreationException e) {
            throw new IllegalStateException("Cannot create self signed certificate", e);
        }
    }

    private KeyStore loadCertificate(Path keyStorePath, char[] keyStorePass) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(keyStorePath.toAbsolutePath().toString());
            keyStore.load(fis, keyStorePass);
            return keyStore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalStateException("Cannot load certificate keystore", e);
        }
    }
}
