#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/javadoc.sh
sh $DIR/versions.sh
