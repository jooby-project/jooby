#!/usr/bin/env sh
stagedFiles=$(git diff --staged --diff-filter=d --name-only | grep ".*\(java\|kt\)$")
if [ "$stagedFiles" != "" ]
then
  # top/parent directory:
  projectDir=$(cd "$(dirname "$1")"; pwd)/$(basename "$1")
  # Generate a comma separated file from staged files and prepend the projectDir, required by spotless
  files=""
  count=0
  for file in $stagedFiles; do
    absfilepath="$projectDir$file"
    if [ -f "$absfilepath" ]
    then
      files+="$absfilepath,"
      let count++
    fi
  done

  if (( $count > 0 ))
  then
    echo "formatting ${count} file(s)"
    mvn spotless:apply -DspotlessFiles="$files" -q
    git add $stagedFiles
  fi
fi
