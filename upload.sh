#!/bin/bash

# safety settings
set -u
set -e
set -o pipefail

. upload.config

branch=$(git rev-parse --abbrev-ref HEAD)
outdir="$OUT_PATH/$branch"
echo "outdir: $outdir"

#scp pack.zip "$outdir"
./create-target-all

#scp -r target/all/* "$outdir"
rsync -rvhplt --delete target/all/* "$outdir"
