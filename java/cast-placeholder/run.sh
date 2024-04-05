#!/bin/sh
# Builds and runs the development server

cd "$(dirname "$0")"
cd ..
mvn -am -pl cast-placeholder package && \
  ~/appengine-java-sdk/bin/dev_appserver.sh --address=0.0.0.0 cast-placeholder/target/cast-placeholder-1.0
