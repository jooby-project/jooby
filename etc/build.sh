#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/maven.sh -T C1 clean checkstyle:checkstyle -P checkstyle package
