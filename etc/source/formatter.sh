#!/usr/bin/env sh
mkdir -p tmp
stagedFiles=$(git diff --staged --name-only | grep ".*\(java\|kt\)$")
if [ "$stagedFiles" != "" ]
then
  # top/parent directory:
  projectDir=$(cd "$(dirname "$1")"; pwd)/$(basename "$1")
  # Generate a comma separated file from staged files and prepend the projectDir, required by spotless
  files=""
  lineFiles=""
  count=0
  for file in $stagedFiles; do
    absfilepath="$projectDir$file"
    if [ -f "$absfilepath" ]
    then
      echo "found ${absfilepath}"
      files+="$absfilepath,"
      lineFiles+="$absfilepath\n"
      let count++
    else
      echo "not found ${absfilepath}"
    fi
  done

  if [ count > 0 ]
  then
    echo "formatting ${count} file(s)"
    mvn spotless:apply -DspotlessFiles="$files" -q
    echo "git add ${lineFiles}"
    git add $lineFiles
  fi
fi
