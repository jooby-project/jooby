#!/bin/bash
mvn -q -Dlogback.configurationFile=../../build/logback-travis.xml -Dcoverage.port=random -Dcoverage.securePort=random -Dsonar.organization=jooby -Dsonar.host.url=https://sonarcloud.io -Dsonar.branch.name=master clean install coveralls:report -P coverage
