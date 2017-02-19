#!/bin/bash
mvn -Dlogback.configurationFile=logback-travis.xml -DdryRun=true clean package coveralls:report -P coverage
