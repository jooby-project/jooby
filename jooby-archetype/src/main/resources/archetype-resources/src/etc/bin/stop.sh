APP_NAME=${project.build.finalName}.jar

echo "Stopping $APP_NAME"

PID=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')

kill $PID
