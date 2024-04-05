#!/bin/sh

cd $(dirname "$0")/..
set -ex

mvn -am -pl appengine-api install
