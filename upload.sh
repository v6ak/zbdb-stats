#!/bin/bash

# safety settings
set -u
set -e
set -o pipefail

. upload.config

./pack.sh

branch=$(git rev-parse --abbrev-ref HEAD)
outdir="$OUT_PATH/$branch"
echo "outdir: $outdir"

#scp pack.zip "$outdir"
if [ -e target/all ]; then
	rm -r target/all
fi
mkdir target/all
(cd target/all; unzip ../../pack.zip)
ln pack.zip target/all/pack.zip

#scp -r target/all/* "$outdir"
rsync -rvhplt --delete target/all/* "$outdir"
