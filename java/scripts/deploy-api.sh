#!/bin/sh
TARGET=appengine-api/target/appengine-api-1.0
TAG=$(dirname "$0")/tag-server.sh

if [ $# -lt 1 ]
then
    echo "usage: deploy-api [staging|production]"
    exit
fi
ENV=$1
echo "Deploying to: $1"

cp "$TARGET/WEB-INF/cron-$ENV.xml" $TARGET/WEB-INF/cron.xml


# appcfg.sh -A "present-$ENV" update $TARGET

gcloud beta app deploy --quiet --project=present-$ENV $TARGET

rm $TARGET/WEB-INF/cron.xml

sh $TAG $ENV
