package net.magnesiumbackend.transport.tomcat;

import net.magnesiumbackend.core.security.SslConfig;
import org.apache.catalina.connector.Connector;

import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public final class TomcatSslAdapter {

    private TomcatSslAdapter() {}

    public static void applyTo(SslConfig config, Connector connector) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        File tmp = File.createTempFile("magnesium-ssl-", ".p12");
        tmp.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            config.keyStore().store(fos, config.password());
        }

        SSLHostConfig sslHostConfig = new SSLHostConfig();
        SSLHostConfigCertificate cert = new SSLHostConfigCertificate(
            sslHostConfig, SSLHostConfigCertificate.Type.RSA
        );
        cert.setCertificateKeystoreFile(tmp.getAbsolutePath());
        cert.setCertificateKeystorePassword(new String(config.password()));
        cert.setCertificateKeystoreType("PKCS12");
        sslHostConfig.addCertificate(cert);

        connector.setScheme("https");
        connector.setSecure(true);
        connector.setProperty("SSLEnabled", "true");
        connector.addSslHostConfig(sslHostConfig);
    }
}