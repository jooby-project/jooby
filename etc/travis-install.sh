#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/maven.sh clean install -q -DskipTests -s $DIR/travis-settings.xml
