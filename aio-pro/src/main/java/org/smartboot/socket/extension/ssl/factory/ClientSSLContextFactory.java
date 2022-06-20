package org.smartboot.socket.extension.ssl.factory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class ClientSSLContextFactory implements SSLContextFactory {
    private InputStream trustInputStream;
    private String trustPassword;

    public ClientSSLContextFactory() {
    }

    public ClientSSLContextFactory(InputStream trustInputStream, String trustPassword) {
        this.trustInputStream = trustInputStream;
        this.trustPassword = trustPassword;
    }

    @Override
    public SSLContext create() throws Exception {
        TrustManager[] trustManagers;
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
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext;
    }
}