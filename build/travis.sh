#!/bin/bash
mvn -q -Dlogback.configurationFile=../../build/logback-travis.xml -Dcoverage.port=random -Dcoverage.securePort=random -Dsonar.organization=jooby -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN -Dsonar.jacoco.reportPaths=../../modules/coverage-report/target/jacoco.exec -Dsonar.branch.name=master clean install coveralls:report sonar:sonar -P coverage
