/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslDemo.java
 * Date: 2020-04-16
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.smartboot.socket;

import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.extension.ssl.factory.ServerSSLContextFactory;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/16
 * <p>
 * 生成证书命令示例:
 *  keytool -genkeypair -keyalg RSA -keysize 2048 -alias root -keystore ca.jks -validity 3650 -storepass 123456 -keypass 123456 -dname CN=RootCA,OU=Test,O=SmartSocket,L=Beijing,ST=Beijing,C=CN
 *
 *  - 生成服务端证书文件。
 *  keytool -genkeypair -keyalg RSA -keysize 2048 -alias server -keystore server.keystore -validity 3650 -storepass 123456 -keypass 123456 -dname CN=Server,OU=Test,O=SmartSocket,L=Beijing,ST=Beijing,C=CN
 *
 *  -生成服务端证书请求文件。
 *  keytool -certreq -alias server -keystore server.keystore -storepass 123456 -file server.csr
 *
 *  - 使用CA根证书签署服务端证书请求。
 *  keytool -gencert -alias root -keystore ca.jks -storepass 123456 -infile server.csr -outfile server.cer
 *
 *  -导出CA根证书。
 *  keytool -exportcert -alias root -keystore ca.jks -storepass 123456 -file ca.cer
 *
 *  -将CA证书导入服务端keystore。
 *  keytool -importcert -alias root -keystore server.keystore -storepass 123456 -file ca.cer -noprompt
 *
 *  -将签署的服务端证书导入服务端keystore。
 *  keytool -importcert -alias server -keystore server.keystore -storepass 123456 -file server.cer -noprompt
 *
 *  -生成客户端证书文件。
 *  keytool -genkeypair -keyalg RSA -keysize 2048 -alias client -keystore client.keystore -validity 3650 -storepass 123456 -keypass 123456 -dname CN=Client,OU=Test,O=SmartSocket,L=Beijing,ST=Beijing,C=CN
 *
 *  -生成客户端证书请求文件。
 *  keytool -certreq -alias client -keystore client.keystore -storepass 123456 -file client.csr
 *
 *  -使用CA根证书签署客户端证书请求。
 *  keytool -gencert -alias root -keystore ca.jks -storepass 123456 -infile client.csr -outfile client.cer
 *
 *  -将CA证书导入客户端keystore。
 *  keytool -importcert -alias root -keystore client.keystore -storepass 123456 -file ca.cer -noprompt
 *
 *  -将签署的客户端证书导入客户端keystore。
 *  keytool -importcert -alias client -keystore client.keystore -storepass 123456 -file client.cer -noprompt
 *
 *  -创建服务端信任库并导入CA证书。
 *  keytool -importcert -alias root -keystore server.truststore -storepass 123456 -file ca.cer -noprompt
 *
 *  -创建客户端信任库并导入CA证书。
 *  keytool -importcert -alias root -keystore client.truststore -storepass 123456 -file ca.cer -noprompt
 */
public class SslDemo {
    public static void main(String[] args) throws Exception {
        IntegerServerProcessor serverProcessor = new IntegerServerProcessor();
        AioQuickServer sslQuickServer = new AioQuickServer(8080, new IntegerProtocol(), serverProcessor);
        ServerSSLContextFactory serverFactory = new ServerSSLContextFactory(SslDemo.class.getClassLoader().getResourceAsStream("server.keystore"), "123456", "123456", SslDemo.class.getClassLoader().getResourceAsStream("server.truststore"), "123456");
        SslPlugin<Integer> sslServerPlugin = new SslPlugin<>(serverFactory, ClientAuth.REQUIRE);
        serverProcessor.addPlugin(sslServerPlugin);
        sslQuickServer.start();

        IntegerClientProcessor clientProcessor = new IntegerClientProcessor();
        AioQuickClient sslQuickClient = new AioQuickClient("localhost", 8080, new IntegerProtocol(), clientProcessor);
        ClientSSLContextFactory clientFactory = new ClientSSLContextFactory(
                SslDemo.class.getClassLoader().getResourceAsStream("client.truststore"), "123456",
                SslDemo.class.getClassLoader().getResourceAsStream("client.keystore"), "123456");
        SslPlugin<Integer> sslPlugin = new SslPlugin<>(clientFactory);
        clientProcessor.addPlugin(sslPlugin);
        AioSession aioSession = sslQuickClient.start();
        aioSession.writeBuffer().writeInt(1);
        aioSession.writeBuffer().flush();

    }
}
