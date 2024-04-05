#!/bin/sh

# Build the Present app
set -x

OUT=../ios/Present/Present/Proto/

FILES="
./present/activity.proto
./present/content.proto
./present/core.proto
./present/group.proto
./present/headers.proto
./present/live.proto
./present/messaging.proto
./present/ping.proto
./present/rpc.proto
./present/url.proto
./present/user.proto
"

protoc --swift_out=$OUT $FILES

cd $OUT
bundle install
bundle exec pod install --repo-update
