#!/usr/bin/env sh
mkdir -p tmp
stagedFiles=$(git diff --staged --name-only | grep ".*\(java\|kt\)$")
if [ "$stagedFiles" != "" ]
then
  # top/parent directory:
  projectDir=$(cd "$(dirname "$1")"; pwd)/$(basename "$1")
  # Generate a comma separated file from staged files and prepend the projectDir, required by spotless
  files=""
  count=0
  for file in $stagedFiles; do
    files+="$projectDir$file,"
    let count++
  done

  echo "formatting ${count} file(s)"
  mvn spotless:apply -DspotlessFiles="$files" -q
  git add $stagedFiles
fi
