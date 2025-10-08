package org.smartboot.socket.extension.ssl.factory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class ClientSSLContextFactory implements SSLContextFactory {
    private InputStream trustInputStream;
    private String trustPassword;
    private InputStream keyInputStream;
    private String keyPassword;

    public ClientSSLContextFactory() {
        // Default constructor for non-mutual authentication
    }


    public ClientSSLContextFactory(InputStream trustInputStream, String trustPassword, InputStream keyInputStream, String keyPassword) {
        this.trustInputStream = trustInputStream;
        this.trustPassword = trustPassword;
        this.keyInputStream = keyInputStream;
        this.keyPassword = keyPassword;

        if (keyInputStream != null && trustInputStream == null) {
            throw new IllegalArgumentException("When keyInputStream is provided, trustInputStream must also be provided for mutual authentication");
        }
    }

    @Override
    public SSLContext create() throws Exception {
        TrustManager[] trustManagers;
        KeyManager[] keyManagers = null;

        // Initialize TrustManagers
        if (trustInputStream != null) {
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(trustInputStream, trustPassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);
            trustManagers = tmf.getTrustManagers();
        } else {
            trustManagers = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
        }

        // Initialize KeyManagers for client authentication
        if (keyInputStream != null) {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(keyInputStream, keyPassword.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyPassword.toCharArray());
            keyManagers = kmf.getKeyManagers();
        }

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    @Override
    public void initSSLEngine(SSLEngine sslEngine) {
        sslEngine.setUseClientMode(true);
    }
}