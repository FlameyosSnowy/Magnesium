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
import java.util.Base64;
import java.util.Collection;
import java.util.Date;

public final class SslConfig {

    private final KeyStore keyStore;
    private final char[]   password;
    private final boolean  webSocketSecured;

    private SslConfig(KeyStore keyStore, char[] password, boolean webSocketSecured) {
        this.keyStore        = keyStore;
        this.password        = password;
        this.webSocketSecured = webSocketSecured;
    }

    public KeyStore keyStore()          { return keyStore; }
    public char[]   password()          { return password; }
    public boolean  isWebSocketSecured(){ return webSocketSecured; }

    // ── PEM ──────────────────────────────────────────────────────────────────

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

    // ── Shared helpers ────────────────────────────────────────────────────────

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
        X509Certificate[] chain = certs.stream()
            .map(X509Certificate.class::cast)
            .toArray(X509Certificate[]::new);

        PrivateKey privateKey;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(keyStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
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