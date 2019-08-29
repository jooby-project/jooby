#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/maven.sh clean install -DskipTests=true
sh $DIR/maven.sh checkstyle:checkstyle -P checkstyle package
