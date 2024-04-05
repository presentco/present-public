#!/bin/sh
# Creates a new live-server cluster from scratch within the given project.

if [ "$#" != "2" ]
then
    echo "usage: create-cluster.sh [project name] [static IP]"
    exit
fi

set -ex
cd $(dirname "$0")

PROJECT=$1
CLUSTER=live-cluster
DEPLOYMENT=live-server
STATIC_IP=$2
PORT=8888

gcloud config set project $PROJECT
gcloud config set compute/zone us-central1-a
source ./push-image.sh $PROJECT
gcloud container clusters create $CLUSTER --num-nodes=1
kubectl run $DEPLOYMENT --image=$IMAGE --port $PORT
kubectl expose deployment $DEPLOYMENT --type=LoadBalancer --port $PORT --load-balancer-ip=$STATIC_IP
kubectl get services