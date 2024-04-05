#!/bin/sh

if [ "$BUILD_WEB" = "false" ]; then
  echo "Skipping web build..."
  exit
fi

cd $(dirname $0)
npm install
npm run build-css
npm run build