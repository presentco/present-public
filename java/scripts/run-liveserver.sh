COLOR=$(dirname "$0")/color-dev-log.sh  # Use ack to color the output if available
java -jar live-server/target/live-server.jar 2>&1 | $COLOR
