#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/maven.sh -pl '!docs,!tests' clean deploy -P bom,central,publishPlugins
