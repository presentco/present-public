#!/bin/sh

APP=appengine-api/target/appengine-api-1.0
filename="api_"`date +%Y_%m_%d`".log"
NUM_DAYS=1 # since midnight 
SEVERITY=0
appcfg.sh \
    --include_all --severity=$SEVERITY --num_days=$NUM_DAYS \
    -A present-gabrielle request_logs $APP \
    "$filename" 

# Filter and colorize a copy
COLOR=$(dirname "$0")/color-gae-log.sh  # Use ack to color the output if available
FILTER=$(dirname "$0")/filter-gae-log.sh  
cat "$filename" | $FILTER | $COLOR > "${filename}.readable"

