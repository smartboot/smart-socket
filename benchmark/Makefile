VERSION=0.1
local:
	mvn clean package
	cp ./target/benchmark-1.0.0.jar benchmark.jar
	docker build --no-cache -t smartboot/smart-socket-benchmark:${VERSION}  .
	rm -f benchmark.jar
