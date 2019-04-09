#!/bin/bash

mvn -pl '!docs,!tests,!examples' checkstyle:checkstyle -P checkstyle
