#!/bin/bash

mvn -pl '!docs,!tests,!examples' javadoc:javadoc -P source
