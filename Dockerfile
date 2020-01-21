# Run tests inside docker

# Usage:
# - docker build -t jooby .
# - docker run -it jooby
# - /build # mvn clean package

FROM ubuntu:latest as ubuntu

# Install OpenJDK-8
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get clean;

# Fix certificate issues
RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

RUN apt-get install -y git

# install wget
RUN apt-get install -y wget

# get maven 3.3.9
RUN wget --no-verbose -O /tmp/apache-maven-3.6.3.tar.gz http://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz

# install maven
RUN tar xzf /tmp/apache-maven-3.6.3.tar.gz -C /opt/
RUN ln -s /opt/apache-maven-3.6.3 /opt/maven
RUN ln -s /opt/maven/bin/mvn /usr/local/bin
RUN rm -f /tmp/apache-maven-3.6.3.tar.gz
ENV MAVEN_HOME /opt/maven

WORKDIR /build
COPY pom.xml .
COPY docs/ /build/docs/
COPY etc/ /build/etc/
COPY jooby/ /build/jooby/
COPY modules/ /build/modules/
COPY tests/ /build/tests/

CMD ["/bin/bash"]
