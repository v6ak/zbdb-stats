#!/bin/bash

# safety settings
set -u
set -e
set -o pipefail

sbt "project server" dist

version=$(
    sbt version | tail -n1 | sed 's/^.* //' | grep -oE '[0-9.A-Z-]+' | head -n2 | tail -n1
)
scalaVersion=$(
    sbt server/scalaVersion | tail -n1 | sed 's/^.* //' | grep -oE '[0-9.A-Z-]+' | head -n2 | tail -n1 | sed 's/^\([^.]*.[^.]*\).*/\1/'
)

if [ -e target/assets ]; then
    rm -r target/assets
fi
mkdir -p target
mkdir target/assets
unzip -d target/assets "./server/target/scala-$scalaVersion/zbdb-stats-server_$scalaVersion-$version-web-assets.jar"


if [ -e pack.zip ]; then
	rm pack.zip
fi

(
  cd target/assets/public &&
    find -name '*.js' | xargs ls -lh &&
    zip -r ../../../pack.zip .
)
