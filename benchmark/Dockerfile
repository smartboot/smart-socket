FROM maven:3.8.6-openjdk-11-slim as build
COPY . .
#RUN mvn -f smart-socket/smart-socket-parent/pom.xml clean install
RUN mvn -f pom.xml package

#RUN make
#FROM openjdk:11
#WORKDIR /
#COPY benchmark.jar benchmark.jar
#EXPOSE 8080
#CMD echo 200000000 > /proc/sys/fs/file-max
#CMD echo 200000000 > /proc/sys/net/netfilter/nf_conntrack_max
#ENTRYPOINT ["java","-cp","benchmark.jar"]