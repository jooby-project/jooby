#!/bin/bash
mvn -DdryRun=true clean package coveralls:report -P coverage
