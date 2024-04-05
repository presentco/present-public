#!/bin/sh

if [ "$#" != "1" ]
then
  echo "Usage: rebuild.sh [module]"
  exit
fi

mvn -am -pl $1 clean
mvn -am -pl $1 package
