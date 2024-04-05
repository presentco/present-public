#!/bin/sh
DEPLOY=$(dirname "$0")/deploy-api.sh

printf "Deploy to production? (y/n)? "
read answer
if echo "$answer" | grep -iq "^y" ;then
    echo "Deploying..."
else
    echo "Exiting."; exit
fi

sh $DEPLOY production

