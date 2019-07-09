#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

if sh $DIR/javadoc.sh; then
  sh $DIR/versions.sh
fi
