if [ -f "animation-board-v2.jar" ]; then
  nohup java -Xms 128m -Xmx 256m -jar animation-board-v2.jar > SERVER.LOG & > /dev/null
  echo $! > PID
  echo "Animation Board v2 web server started."
else
  echo "Cannot find animation-board-v2.jar ."
  exit 1
fi