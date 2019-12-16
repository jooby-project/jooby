#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

STARTERS="!starters,!starters/graphql-starter,!starters/pac4j-starter"

sh $DIR/maven.sh -pl '!docs,!tests,!examples,$STARTERS' clean deploy -P bom,central
