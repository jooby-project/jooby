#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

sh $DIR/maven.sh -pl '!docs,!tests,!examples,!starters' clean deploy -P bom,central
