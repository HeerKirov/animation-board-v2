if [ -f "PID" ]; then
  nohup java -jar animation-board-v2.jar >> SERVER.LOG &
  echo $! > PID
  echo "Animation Board v2 web server started."
else
  echo "Cannot find animation-board-v2.jar ."
  exit 1
fi