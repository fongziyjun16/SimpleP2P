FROM ubuntu:22.04
WORKDIR /opt
ADD jdk-8u341-linux-x64.tar.gz /opt/java/
ENV JAVA_HOME=/opt/java/jdk1.8.0_341
ENV PATH=$JAVA_HOME/bin:$PATH
RUN apt-get update && apt-get install -y iputils-ping net-tools