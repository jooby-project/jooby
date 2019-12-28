#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

# Update starter version
mvn clean package -N -DJOOBY_VERSION="$@"

# Now build
mvn clean package
