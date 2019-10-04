#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/maven.sh -Dlogback.configurationFile=$DIR/logback-travis.xml -q checkstyle:checkstyle -P checkstyle package
