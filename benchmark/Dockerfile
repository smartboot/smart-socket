FROM maven:3.8.6-openjdk-11-slim as build
COPY . .
RUN mvn -f pom.xml package
RUN mv target/benchmark-*.jar benchmark.jar

FROM openjdk:11
WORKDIR /
COPY --from=build benchmark.jar benchmark.jar
EXPOSE 8080
## 在宿主机执行以下命令
CMD echo 200000000 > /proc/sys/fs/file-max
CMD echo 200000000 > /proc/sys/net/netfilter/nf_conntrack_max
ENTRYPOINT ["java","-cp","benchmark.jar"]