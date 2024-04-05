#!/bin/sh

if [ $# -lt 1 ]
then
    echo "usage: tag-server [staging|production]"
    exit
fi

REV=`git rev-parse --short=5 HEAD`
ENV=$1
TAG="Server-$ENV-$REV"

if git tag | grep -q "$TAG"
then
    echo "Build \"$TAG\" already tagged"; exit
fi

echo "Tagging: $TAG"
git tag $TAG
git push origin refs/tags/$TAG
