if [ -f "PID" ]; then
    kill $(cat PID)
    rm PID
fi