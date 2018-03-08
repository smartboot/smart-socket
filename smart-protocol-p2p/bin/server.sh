#!/bin/sh
java -cp ../conf -Djava.ext.dirs=../lib/ org.smartboot.socket.protocol.p2p.server.P2PServer
