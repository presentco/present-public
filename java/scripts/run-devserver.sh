#!/bin/sh

APP=appengine-ear/target/appengine-ear-1.0  # Run from the ear
#APP=appengine-api/target/appengine-api-1.0
#PORT="--port=8081"

COLOR=$(dirname "$0")/color-dev-log.sh  # Use ack to color the output if available

# Run from the exploded api module
dev_appserver.sh \
    --address=0.0.0.0 \
    $PORT \
    --jvm_flag=-Dappengine.fullscan.seconds=5 \
    "$APP" 2>&1 | $COLOR | tee devserver.log

#datastore.default_high_rep_job_policy_unapplied_job_pct=20

