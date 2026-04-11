package net.magnesiumbackend.transport.undertow;

import io.undertow.Undertow;
import net.magnesiumbackend.core.security.SslConfig;
import org.jetbrains.annotations.NotNull;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public final class UndertowSslAdapter {

    private UndertowSslAdapter() {}

    public static void applyTo(
        @NotNull SslConfig config,
        @NotNull Undertow.Builder builder,
        int port,
        String host
    ) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        builder.addHttpsListener(port, host, config.sslContext());
    }
}