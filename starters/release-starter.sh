#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

# Update starter version
mvn clean package -N

# Now build
mvn clean package
