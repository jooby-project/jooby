#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/maven.sh -T C1 clean install -DskipTests=true
sh $DIR/maven.sh -T C1 checkstyle:checkstyle -P checkstyle package
