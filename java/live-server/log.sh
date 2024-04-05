#!/bin/sh
# Follow live-server logs for the given project

if [ "$#" != "1" ]
then
    echo "usage: log.sh [project name]"
    exit
fi

set -e
cd $(dirname "$0")

PROJECT=$1
CLUSTER=live-cluster
DEPLOYMENT=live-server

gcloud config set project $PROJECT
gcloud container clusters get-credentials $CLUSTER
POD=`kubectl get pods | grep $DEPLOYMENT | cut -d ' ' -f 1`
kubectl logs -f $POD