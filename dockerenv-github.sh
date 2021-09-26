#!/bin/bash
# safety settings
set -u
set -e
set -o pipefail

exec docker run \
  -v "$HOME"/.ivycache:/root/.ivy2 \
  -v "$HOME"/.sbtcache:/root/.sbt \
  -v "$HOME"/.ssh/zbdb-known_hosts:/root/.ssh/known_hosts:ro \
  -v "$HOME"/.ssh/zbdb-id_rsa:/root/.ssh/id_rsa:ro \
  -v "$HOME"/.ssh/zbdb-id_rsa.pub:/root/.ssh/id_rsa.pub:ro \
  -v "$(pwd)":/project \
  -p 9000:9000 \
  "$(docker build -q .)" \
  "$@"
