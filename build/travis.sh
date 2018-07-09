#!/bin/bash
mvn -q -Dlogback.configurationFile=logback-travis.xml -Dcoverage.port=random -Dcoverage.securePort=random clean package coveralls:report -P coverage
