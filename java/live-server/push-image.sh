#!/bin/sh
# Builds and pushes a live-server container image.

if [ "$#" != "1" ]
then
    echo "usage: push-image.sh [project name]"
    exit
fi

set -ex
cd $(dirname "$0")

PROJECT=$1
VERSION=`git rev-parse --short=6 HEAD`
export IMAGE=gcr.io/$PROJECT/live-server:$VERSION

# Run Maven build.
(cd ..; mvn -am -pl live-server package)

# Build and push Docker image.
docker build -t $IMAGE .
gcloud config set project $PROJECT
gcloud docker -- push $IMAGE