#!/bin/bash
mvn -Dlogback.configurationFile=logback-travis.xml -DdryRun=true -Dcoverage.port=random -Dcoverage.securePort=random clean package coveralls:report -P coverage
