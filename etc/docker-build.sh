#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

if [ -x "$(command -v mvnd)" ]; then
  mvnd -P '!git-hooks' clean -P gradlePlugin package
else
  mvn -P '!git-hooks' clean -P gradlePlugin package
fi
