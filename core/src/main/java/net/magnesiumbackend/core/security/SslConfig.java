package net.magnesiumbackend.core.security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Configuration for SSL/TLS certificates and keys.
 *
 * <p>SslConfig provides a unified way to load SSL certificates from various sources:
 * <ul>
 *   <li>PEM files (certificate chain + private key)</li>
 *   <li>Java KeyStore files (JKS or PKCS12)</li>
 *   <li>Self-signed certificates (development only)</li>
 * </ul>
 * </p>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // From PEM files (production)
 * SslConfig ssl = SslConfig.fromPem(
 *     new File("/etc/ssl/cert.pem"),
 *     new File("/etc/ssl/key.pem")
 * );
 *
 * // From Java KeyStore
 * SslConfig ssl = SslConfig.fromKeyStore(
 *     new File("/etc/ssl/keystore.p12"),
 *     "password".toCharArray()
 * );
 *
 * // Self-signed (development only)
 * SslConfig ssl = SslConfig.selfSigned();
 *
 * // Create SSLContext
 * SSLContext ctx = ssl.sslContext();
 * }</pre>
 *
 * @see SSLContext
 * @see KeyManagerFactory
 */
public final class SslConfig {

    private final KeyStore keyStore;
    private final char[]   password;
    private final boolean  webSocketSecured;

    private SslConfig(KeyStore keyStore, char[] password, boolean webSocketSecured) {
        this.keyStore        = keyStore;
        this.password        = password;
        this.webSocketSecured = webSocketSecured;
    }

    /** Returns the loaded KeyStore. */
    public KeyStore keyStore()          { return keyStore; }

    /** Returns the KeyStore password. */
    public char[]   password()          { return password; }

    /** Returns true if WebSocket should use wss://. */
    public boolean  isWebSocketSecured(){ return webSocketSecured; }

    /**
     * Loads SSL config from PEM files with WebSocket secured.
     *
     * @param certChainFile the certificate chain PEM file
     * @param privateKeyFile the private key PEM file
     * @return the SSL configuration
     * @throws Exception if loading fails
     */
    public static SslConfig fromPem(File certChainFile, File privateKeyFile) throws Exception {
        return fromPem(certChainFile, privateKeyFile, true);
    }

    public static SslConfig fromPem(
        File certChainFile,
        File privateKeyFile,
        boolean secureWebSocket
    ) throws Exception {
        KeyStore ks = buildKeyStoreFromPem(
            new FileInputStream(certChainFile),
            new FileInputStream(privateKeyFile)
        );
        return new SslConfig(ks, new char[0], secureWebSocket);
    }

    public static SslConfig fromKeyStore(File keyStoreFile, char[] password) throws Exception {
        return fromKeyStore(keyStoreFile, password, true);
    }

    public static SslConfig fromKeyStore(
        File keyStoreFile,
        char[] password,
        boolean secureWebSocket
    ) throws Exception {
        String type = keyStoreFile.getName().endsWith(".jks") ? "JKS" : "PKCS12";
        KeyStore ks = KeyStore.getInstance(type);
        try (FileInputStream fis = new FileInputStream(keyStoreFile)) {
            ks.load(fis, password);
        }
        return new SslConfig(ks, password, secureWebSocket);
    }

    /** Dev/testing only */
    public static SslConfig selfSigned() throws Exception {
        return selfSigned(false);
    }

    public static SslConfig selfSigned(boolean secureWebSocket) throws Exception {
        KeyStore ks = buildSelfSignedKeyStore();
        return new SslConfig(ks, new char[0], secureWebSocket);
    }

    public KeyManagerFactory keyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        KeyManagerFactory kmf = KeyManagerFactory
            .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        return kmf;
    }

    public SSLContext sslContext() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(keyManagerFactory().getKeyManagers(), null, null);
        return ctx;
    }

    private static KeyStore buildSelfSignedKeyStore() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=localhost, O=Magnesium Dev, C=US");
        BigInteger serial  = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore     = Date.from(Instant.now());
        Date notAfter      = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter()
            .getCertificate(
                new JcaX509v3CertificateBuilder(
                    subject, serial, notBefore, notAfter, subject,
                    keyPair.getPublic()
                ).build(signer)
            );

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("magnesium", keyPair.getPrivate(), new char[0],
            new Certificate[]{ cert });
        return ks;
    }

    private static KeyStore buildKeyStoreFromPem(
        InputStream certStream,
        InputStream keyStream
    ) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certs;
        try (certStream) {
            certs = cf.generateCertificates(certStream);
        }
        List<X509Certificate> list = new ArrayList<>(certs.size());
        for (Certificate cert : certs) {
            X509Certificate x509Certificate = (X509Certificate) cert;
            list.add(x509Certificate);
        }
        X509Certificate[] chain = list.toArray(new X509Certificate[0]);

        PrivateKey privateKey;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(keyStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(32);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("-----")) sb.append(line.strip());
            }
            byte[] keyBytes = Base64.getDecoder().decode(sb.toString());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("magnesium", privateKey, new char[0], chain);
        return ks;
    }
}