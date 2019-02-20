#!/bin/bash

mvn -pl '!docs,!tests,!examples' clean deploy -P bom,central
