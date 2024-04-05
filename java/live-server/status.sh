#!/bin/sh
# Prints the status of the deployment in the given project.

if [ "$#" != "1" ]
then
    echo "usage: status.sh [project name]"
    exit
fi

set -e

PROJECT=$1
CLUSTER=live-cluster
DEPLOYMENT=live-server

gcloud config set project $PROJECT
gcloud container clusters get-credentials $CLUSTER
kubectl get deployments -o wide
