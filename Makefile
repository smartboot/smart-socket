# 当需要升级版本时，执行该命令
version=1.8.1
update_version:
	sed -i  '' 's/public static final String VERSION = ".*";/public static final String VERSION = "v${version}";/' aio-core/src/main/java/org/smartboot/socket/transport/IoServerConfig.java
	mvn -f smart-socket-parent/pom.xml versions:set -DnewVersion=${version} versions:commit
	mvn -f smart-socket-parent/pom.xml clean install
	mvn versions:use-dep-version -Dincludes=io.github.smartboot.socket:smart-socket-parent -DdepVersion=${version} versions:commit
	mvn -f benchmark/pom.xml versions:use-dep-version -Dincludes=io.github.smartboot.socket:aio-pro -DdepVersion=${version} versions:commit
	mvn -f example/pom.xml versions:use-dep-version -Dincludes=io.github.smartboot.socket:aio-pro -DdepVersion=${version} versions:commit

