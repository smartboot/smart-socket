package org.smartboot.socket.extension.ssl.factory;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 自签名SSL证书工厂类，用于自动生成服务端SSL证书
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2025/10/08
 */
public final class AutoServerSSLContextFactory implements SSLContextFactory {
    private final String commonName;
    private final String organization;
    private final String organizationalUnit;
    private final int validityDays;
    private final String keyAlgorithm;
    private final int keySize;

    /**
     * 构造函数
     *
     * @param commonName         通用名称 (CN)
     * @param organization       组织名称 (O)
     * @param organizationalUnit 组织单元名称 (OU)
     * @param validityDays       证书有效期（天）
     * @param keyAlgorithm       密钥算法 (如 RSA, DSA, EC)
     * @param keySize            密钥长度
     */
    AutoServerSSLContextFactory(String commonName, String organization, String organizationalUnit,
                                int validityDays, String keyAlgorithm, int keySize) {
        this.commonName = commonName;
        this.organization = organization;
        this.organizationalUnit = organizationalUnit;
        this.validityDays = validityDays;
        this.keyAlgorithm = keyAlgorithm;
        this.keySize = keySize;
    }

    /**
     * 默认构造函数，使用默认配置
     */
    public AutoServerSSLContextFactory() {
        this("localhost", "smartboot", "smart-socket", 365, "RSA", 2048);
    }

    @Override
    public SSLContext create() throws Exception {
        // 生成密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyAlgorithm);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        // 生成自签名证书
        X509Certificate certificate = generateSelfSignedCertificate(keyPair);

        // 创建KeyStore并存储密钥和证书
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        Certificate[] chain = {certificate};
        keyStore.setKeyEntry("server", privateKey, new char[0], chain);

        // 初始化KeyManagerFactory
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, new char[0]);

        // 创建SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
        return sslContext;
    }

    @Override
    public void initSSLEngine(SSLEngine sslEngine) {
        sslEngine.setUseClientMode(false);
    }

    private X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        // 创建证书信息
        X509CertInfo certInfo = new X509CertInfo();

        // 设置证书版本
        certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));

        // 设置序列号
        certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, new SecureRandom())));

        // 设置签名算法
        AlgorithmId algo = new AlgorithmId(AlgorithmId.sha256WithRSAEncryption_oid);
        certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // 设置有效期
        long currentTime = System.currentTimeMillis();
        Date from = new Date(currentTime);
        Date to = new Date(currentTime + validityDays * 24L * 60 * 60 * 1000);
        CertificateValidity interval = new CertificateValidity(from, to);
        certInfo.set(X509CertInfo.VALIDITY, interval);

        // 设置主体和颁发者
        String dn = "CN=" + commonName + ", O=" + organization + ", OU=" + organizationalUnit;
        certInfo.set(X509CertInfo.SUBJECT, new X500Name(dn));
        certInfo.set(X509CertInfo.ISSUER, new X500Name(dn));

        // 设置公钥
        certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));

        // 设置扩展信息（可选）
        CertificateExtensions extensions = new CertificateExtensions();
        // 可以添加更多扩展信息
        certInfo.set(X509CertInfo.EXTENSIONS, extensions);

        // 创建并签名证书
        X509CertImpl cert = new X509CertImpl(certInfo);
        cert.sign(keyPair.getPrivate(), "SHA256withRSA");

        return cert;

    }
}