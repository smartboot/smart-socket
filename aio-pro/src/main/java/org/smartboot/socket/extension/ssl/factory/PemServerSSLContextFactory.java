package org.smartboot.socket.extension.ssl.factory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
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
    private final File pemFile;

    public PemServerSSLContextFactory(File pemFile) {
        this.pemFile = pemFile;
    }

    @Override
    public SSLContext create() throws Exception {

        List<Certificate> certificates = new ArrayList<>();
        PrivateKey privateKey = null;
        // 生成证书
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // 读取PEM文件
        FileReader fileReader = new FileReader(pemFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            switch (line) {
                case "-----BEGIN CERTIFICATE-----":
                case "-----BEGIN PRIVATE KEY-----":
                    sb.setLength(0);
                    break;
                case "-----END CERTIFICATE-----": {
                    byte[] bytes = Base64.getDecoder().decode(sb.toString());
                    certificates.add(cf.generateCertificate(new ByteArrayInputStream(bytes)));
                    break;
                }
                case "-----END PRIVATE KEY-----": {
                    byte[] bytes = Base64.getDecoder().decode(sb.toString());
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
                    privateKey = keyFactory.generatePrivate(keySpec);
                    break;
                }
                default:
                    sb.append(line);
                    break;
            }
        }

        // 生成KeyStore
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry("keyAlias", privateKey, new char[0], certificates.toArray(new Certificate[0]));

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(ks, "".toCharArray());

        // 生成SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }
}
