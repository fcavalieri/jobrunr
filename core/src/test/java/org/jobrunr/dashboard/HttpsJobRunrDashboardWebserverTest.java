package org.jobrunr.dashboard;

import org.jobrunr.utils.FreePortFinder;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.testcontainers.shaded.org.bouncycastle.asn1.x500.X500Name;
import org.testcontainers.shaded.org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.testcontainers.shaded.org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testcontainers.shaded.org.bouncycastle.operator.ContentSigner;
import org.testcontainers.shaded.org.bouncycastle.operator.OperatorCreationException;
import org.testcontainers.shaded.org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class HttpsJobRunrDashboardWebserverTest extends JobRunrDashboardWebServerTest {

    @Override
    public JsonMapper getJsonMapper() {
        return new GsonJsonMapper();
    }

    @Override
    public JobRunrDashboardWebServerConfiguration getDashboardConfiguration() {
        try {
        int portHttp = FreePortFinder.nextFreePort(8000);
        int portHttps = FreePortFinder.nextFreePort(portHttp + 1);
        String keyStorePassword = "test";
        File keyStoreFile = File.createTempFile("jobrunr-", "-certificate");
        keyStoreFile.deleteOnExit();
        generateKeyStore(keyStoreFile, keyStorePassword);
        return JobRunrDashboardWebServerConfiguration
                .usingStandardDashboardConfiguration()
                .andPort(portHttp)
                .andPortHttps(portHttps)
                .andEnableHttp(true)
                .andEnableHttps(true)
                .andKeyStoreHttps(keyStoreFile.toPath(), keyStorePassword);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void generateKeyStore(File keyStoreFile, String keyStorePassword) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            X500Name dnName = new X500Name("CN=localhost");
            BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());
            String signatureAlgorithm = "SHA256WithRSA";
            ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
                    .build(keyPair.getPrivate());
            Instant startDate = Instant.now();
            Instant endDate = startDate.plus(25 * 365, ChronoUnit.DAYS);
            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    dnName, certSerialNumber, Date.from(startDate), Date.from(endDate), dnName,
                    keyPair.getPublic());
            Certificate certificate = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(certBuilder.build(contentSigner));
            PrivateKey privateKey = keyPair.getPrivate();

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            Certificate[] chain = new Certificate[]{certificate};
            keyStore.setKeyEntry("alias", privateKey, keyStorePassword.toCharArray(), chain);
            keyStore.store(new FileOutputStream(keyStoreFile), keyStorePassword.toCharArray());
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | NoSuchProviderException | OperatorCreationException e) {
            throw new IllegalStateException("Cannot create self signed certificate", e);
        }
    }

}
