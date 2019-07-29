#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

if sh $DIR/checkstyle.sh; then
  sh $DIR/versions.sh
fi
