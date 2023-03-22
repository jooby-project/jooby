# Run tests inside docker

# Usage:
# - docker build -t jooby .
# - docker run -it jooby
# - /build # mvn clean package

FROM ubuntu:latest

RUN apt-get update && \
    apt-get install -y openjdk-17-jdk && \
    apt-get clean;

RUN apt-get install -y wget

RUN wget --no-verbose -O /tmp/apache-maven-3.9.1.tar.gz https://dlcdn.apache.org/maven/maven-3/3.9.1/binaries/apache-maven-3.9.1-bin.tar.gz

# install maven
RUN tar xzf /tmp/apache-maven-3.9.1.tar.gz -C /opt/
RUN ln -s /opt/apache-maven-3.9.1 /opt/maven
RUN ln -s /opt/maven/bin/mvn /usr/local/bin
RUN rm -f /tmp/apache-maven-3.9.1.tar.gz
ENV MAVEN_HOME /opt/maven

# Random port for testing

ENV BUILT_PORT 0
ENV BUILT_SECURE_PORT 0

WORKDIR /build

COPY pom.xml .
COPY docs/ /build/docs/
COPY etc/ /build/etc/
COPY jooby/ /build/jooby
COPY modules/ /build/modules/
COPY tests/ /build/tests/

# Install
RUN mvn clean install --fail-never -q

CMD ["/bin/bash"]
