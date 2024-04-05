#!/bin/sh
# Connects to the remote dashboard

if [ "$#" != "1" ]
then
    echo "usage: dashboard.sh [project name]"
    exit
fi

set -e
cd $(dirname "$0")

PROJECT=$1
CLUSTER=live-cluster
DEPLOYMENT=live-server

gcloud container clusters get-credentials $CLUSTER --project $PROJECT
open http://localhost:8001/ui
kubectl proxy