#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

if [ -x "$(command -v mvnd)" ]; then
  mvnd -pl '!docs' -P '!git-hooks' clean -P gradlePlugin package
else
  mvn  -pl '!docs'-P '!git-hooks' clean -P gradlePlugin package
fi
