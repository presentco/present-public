#!/bin/sh
# Re-deploys live-server to the given project

if [ "$#" != "1" ]
then
    echo "usage: deploy.sh [project name]"
    exit
fi

set -ex
cd $(dirname "$0")

PROJECT=$1
CLUSTER=live-cluster
DEPLOYMENT=live-server

source ./push-image.sh
gcloud container clusters get-credentials $CLUSTER
kubectl set image deployment/$DEPLOYMENT $DEPLOYMENT=$IMAGE
kubectl rollout status deployment/$DEPLOYMENT
kubectl get deployment/$DEPLOYMENT -o=wide