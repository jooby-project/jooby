#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/maven.sh clean checkstyle:checkstyle -P checkstyle package
