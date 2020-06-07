if [ -f "PID" ]; then
  java -Dloader.main=com.heerkirov.animation.command.TransformDatabaseKt -jar animation-board-v2.jar
else
  echo "Cannot find animation-board-v2.jar ."
  exit 1
fi