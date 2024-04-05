#!/bin/sh
DEPLOY=$(dirname "$0")/deploy-api.sh
sh $DEPLOY staging
