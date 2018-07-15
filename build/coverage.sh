#!/bin/bash
mvn -DdryRun=true -Dlogback.configurationFile=../../build/logback-build.xml  clean package coveralls:report -P coverage
