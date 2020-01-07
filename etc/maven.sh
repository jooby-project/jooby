#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

mvn -pl '!docs' "$@"
