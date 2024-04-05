#!/bin/sh
appcfg.sh -A present-staging update_indexes appengine-api/target/appengine-api-1.0
echo "Monitor progress at:  https://console.cloud.google.com/datastore/indexes"

