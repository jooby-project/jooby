# Run tests inside docker

# Usage:
# - docker build -t jooby .
# - docker run -it --rm jooby /bin/sh
# - /build # mvn clean package

FROM maven:3.6.0-jdk-8-alpine as maven

WORKDIR /build
COPY pom.xml .
COPY docs/ /build/docs/
COPY etc/ /build/etc/
COPY examples/ /build/examples/
COPY jooby/ /build/jooby/
COPY modules/ /build/modules/
COPY tests/ /build/tests/

RUN mvn clean install -q -DskipTests=true
