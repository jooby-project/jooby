#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

if sh $DIR/maven.sh checkstyle:checkstyle -P checkstyle; then
  sh $DIR/javadoc.sh
fi
