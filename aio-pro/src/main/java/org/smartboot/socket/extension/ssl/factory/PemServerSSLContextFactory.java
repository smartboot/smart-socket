package org.smartboot.socket.extension.ssl.factory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PemServerSSLContextFactory implements SSLContextFactory {
    private List<byte[]> certificates = new ArrayList<>();
    private byte[] keyBytes;

    public PemServerSSLContextFactory(InputStream fullPem) throws IOException {
        readPem(fullPem);
    }

    public PemServerSSLContextFactory(InputStream certPem, InputStream keyPem) throws IOException {
        readPem(certPem);
        readPem(keyPem);
    }

    @Override
    public SSLContext create() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // 证书
        Certificate[] chain = new Certificate[certificates.size()];
        for (int i = 0; i < certificates.size(); i++) {
            chain[i] = cf.generateCertificate(new ByteArrayInputStream(certificates.get(i)));
        }
        //私钥
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        // 生成KeyStore
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry("keyAlias", privateKey, new char[0], chain);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(ks, "".toCharArray());

        // 生成SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        certificates = null;
        keyBytes = null;
        return sslContext;
    }

    private void readPem(InputStream inputStream) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            switch (line) {
                case "-----BEGIN CERTIFICATE-----":
                case "-----BEGIN PRIVATE KEY-----":
                    sb.setLength(0);
                    break;
                case "-----END CERTIFICATE-----": {
                    certificates.add(Base64.getDecoder().decode(sb.toString()));
                    break;
                }
                case "-----END PRIVATE KEY-----": {
                    keyBytes = Base64.getDecoder().decode(sb.toString());
                    break;
                }
                default:
                    sb.append(line);
                    break;
            }
        }
    }
}
