#!/usr/bin/env bash
## PATH Separator
case "`uname`" in
CYGWIN*) PATH_SEPARATOR=";";;
*) PATH_SEPARATOR=":";;
esac

## application
APP_ENV=${1:-dev}
APP_MAIN=${application.main}
APP_NAME=${project.build.finalName}.jar

## logback file
if [ -f "config/logback.${APP_ENV}.xml" ];
then
  LOGBACK_FILE="config/logback.${APP_ENV}.xml"
else
  LOGBACK_FILE="config/logback.xml"
fi

## secret
if [ -z "$APP_SECRET" ]
then
  # try current dir
  if [ -f ".secret" ];
  then
    APP_SECRET=$(cat ".secret")
  else
    if [ -f "$HOME/.secret" ];
    then
      APP_SECRET=$(cat "$HOME/.secret")
    fi
  fi
fi

APP_OPTIONS="-Dapplication.secret=${APP_SECRET} -Dapplication.env=${APP_ENV} -Dlogback.configurationFile=$LOGBACK_FILE -Djava.io.tmpdir=tmp"

JAVA_OPTIONS="-Xms512m -Xmx1024m"

CP="-cp public${PATH_SEPARATOR}config$PATH_SEPARATOR$APP_NAME"

VM_ARGS="$JAVA_OPTIONS $APP_OPTIONS $CP $APP_MAIN"

echo "Environment: $APP_ENV"
echo "Logback file: $LOGBACK_FILE"
echo "JVM options: $JAVA_OPTIONS"
echo "Starting $APP_MAIN"

nohup java $VM_ARGS > /dev/null 2>&1 &
