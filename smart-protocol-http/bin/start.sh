#!/bin/sh
HTTP_HOME=$(dirname $(pwd))
java -Dwebapps.dir=${HTTP_HOME}/webapps -Dlog4j.configurationFile=file:${HTTP_HOME}/conf/log4j2.xml -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${HTTP_HOME}/lib/ org.smartboot.socket.http.HttpBootstrap
