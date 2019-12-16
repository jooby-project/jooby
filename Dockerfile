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
COPY starters/ /build/starters/

RUN rm -rf /build/modules/jooby-graphiql/node
RUN rm -rf /build/modules/jooby-graphiql/node_modules

RUN rm -rf /build/modules/jooby-graphql-playground/node
RUN rm -rf /build/modules/jooby-graphql-playground/node_modules
