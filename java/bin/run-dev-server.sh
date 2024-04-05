#!/bin/sh

# Build and run the web and API servers.
#
# Set SKIP_TESTS=true to skip running the tests.
# Set BUILD_WEB=false to skip building appengine-web.
# Set BUILD=false to skip building altogether.
#
# For example:
#
#  $ SKIP_TESTS=true BUILD_WEB=false ./run-dev-server.sh

set -e
cd $(dirname "$0")/..

COLOR=./scripts/color-dev-log.sh  # Use ack to color the output if available

if [ "$BUILD_WEB" = "false" ]; then
  REPO_WEB=~/.m2/repository/present/appengine-web/

  echo "Warning: Skipping appengine-web build. Using $REPO_WEB instead of ./appengine-web."

  if [ ! -d $REPO_WEB  ]; then
    echo "Installing appengine-web for the first (and only) time..."
    mvn -am -pl appengine-web install
  fi

  PACKAGES='appengine-ear,!appengine-web'
else
  PACKAGES=appengine-ear
fi

if [ "$SKIP_TESTS" = "true" ]; then
  echo "Warning: Skipping tests..."
fi

if [ "$BUILD" != "false" ]; then
  mvn -am -pl $PACKAGES package -DskipTests=${SKIP_TESTS:false}
fi

dev_appserver.sh --jvm_flag=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  appengine-ear/target/appengine-ear-1.0 | $COLOR
