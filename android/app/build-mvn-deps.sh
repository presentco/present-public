#!/bin/sh

#########################
# Execute Maven build to refresh the local maven repository
#########################
cd ../../java; mvn clean install -am -pl proto,wire-rpc-core,wire-rpc-client,live-client