# Run tests inside docker

# Usage:
# - docker build -t jooby .
# - docker run -it --rm jooby /bin/sh
# - /build # mvn clean package

FROM maven:3.6.0-jdk-8 as maven

WORKDIR /build
COPY pom.xml .
COPY docs/ /build/docs/
COPY etc/ /build/etc/
COPY jooby/ /build/jooby/
COPY modules/ /build/modules/
COPY tests/ /build/tests/

RUN rm -rf /build/modules/jooby-graphiql/node
RUN rm -rf /build/modules/jooby-graphiql/node_modules

RUN rm -rf /build/modules/jooby-graphql-playground/node
RUN rm -rf /build/modules/jooby-graphql-playground/node_modules

RUN mvn clean install -DskipTests -q
