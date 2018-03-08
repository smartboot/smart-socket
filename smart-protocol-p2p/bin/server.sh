#!/bin/sh
curPwd=`pwd`
java -Dlog4j.configurationFile=file:${curPwd}/../conf/log4j2_server.xml -Djava.ext.dirs=../lib/ org.smartboot.socket.protocol.p2p.server.P2PServer
