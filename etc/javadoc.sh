#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

mvn javadoc:javadoc -P source -Dmaven.plugin.validation=VERBOSE
