#!/bin/bash

mvn -pl '!modules/coverage-report' clean deploy -P sonatype-oss-release

cd modules/jooby-bom

groovy bom.groovy > pom.xml

# mvn -Dalpn-boot-version=$ALPN_VERSION clean deploy -P sonatype-oss-release
mvn clean deploy -P sonatype-oss-release
