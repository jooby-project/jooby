#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

if [ -x "$(command -v mvnd)" ]; then
  mvnd clean -P gradlePlugin package
else
  mvn  clean -P gradlePlugin package
fi
