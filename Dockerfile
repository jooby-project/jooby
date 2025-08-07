# Run tests inside docker

# Usage:
# - docker build -t jooby .
# - docker run -v "$HOME/.m2":/root/.m2 -it jooby
# - /build # mvn clean package

FROM maven:3-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
COPY docs/ /build/docs/
COPY etc/source/LICENSE /build/etc/source/LICENSE
COPY etc/build.sh /build/etc/build.sh
COPY etc/javadoc.sh /build/etc/javadoc.sh
COPY jooby/ /build/jooby/
COPY modules/ /build/modules/
COPY tests/ /build/tests/

ENV BUILT_PORT=0
ENV BUILT_SECURE_PORT=0

CMD ["/bin/bash"]
