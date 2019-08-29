#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

echo "$@"

mvn -pl '!docs,!examples' "$@"
