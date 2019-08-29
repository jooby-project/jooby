#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

if sh $DIR/javadoc.sh; then
  sh $DIR/maven.sh -pl '!docs,!tests,!examples' clean deploy -P bom,central
fi
