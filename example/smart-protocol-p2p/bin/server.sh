#!/bin/sh
HTTP_HOME=$(dirname $(pwd))
java -Dlog4j.configurationFile=file:${HTTP_HOME}/conf/log4j2_server.xml -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${HTTP_HOME}/lib/ org.smartboot.socket.protocol.p2p.server.P2PServer
