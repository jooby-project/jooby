#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/javadoc.sh

sh $DIR/maven.sh clean deploy -P bom,central
