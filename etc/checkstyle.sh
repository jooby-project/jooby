#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/maven.sh checkstyle:checkstyle -P checkstyle
