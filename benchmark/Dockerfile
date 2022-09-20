FROM openjdk:11
WORKDIR /
COPY benchmark.jar benchmark.jar
EXPOSE 1883
CMD echo 100000000 > /proc/sys/fs/file-max
## 当前镜像不开放修改权限
CMD chmod 777 /proc/sys/net/netfilter/nf_conntrack_max
#CMD echo 100000000 > /proc/sys/net/netfilter/nf_conntrack_max
ENTRYPOINT ["java","-cp","benchmark.jar"]