#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

if [ -x "$(command -v mvnd)" ]; then
  mvnd -pl '!docs' clean -P gradlePlugin package
else
  mvn  -pl '!docs' clean -P gradlePlugin package
fi
