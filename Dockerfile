# Run tests inside docker

# Usage:
# - docker build -t jooby .
# - docker run -v "$HOME/.m2":/root/.m2 -it jooby
# - /build # mvn clean package -P '!git-hooks'

FROM maven:3-eclipse-temurin-17 as build

WORKDIR /build

COPY pom.xml .
COPY docs/ /build/docs/
COPY etc/ /build/etc/
COPY jooby/ /build/jooby/
COPY modules/ /build/modules/
COPY tests/ /build/tests/

ENV BUILT_PORT 0
ENV BUILT_SECURE_PORT 0

CMD ["/bin/bash"]
