#!/bin/sh

cd $(dirname "$0")/..

SCRIPT=`basename $0`

if [ "$#" == "0" ]
then
    echo "usage: $SCRIPT [email] [email]..."
    exit
fi

MAIN=present.server.tool.SendExampleEmails
ARGS="-classpath appengine-api/src/main/resources:%classpath $MAIN $@"
mvn -pl appengine-api exec:exec -Dexec.executable="java" \
  -Dexec.args="$ARGS"