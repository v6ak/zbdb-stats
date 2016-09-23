#!/bin/bash

# safety settings
set -u
set -e
set -o pipefail

YEARS=( 2015 2016 )

#printf "%s.html\n" "${YEARS[@]}"

"$(which sbt activator | head -n1)" fullOptJS

for i in "${YEARS[@]}"; do
	cat -- "$i-dev.html" | sed 's/zbdb-stats-fastopt.js/zbdb-stats-opt\.js/' | sed s/'zbdb-stats-jsdeps\.js'/'zbdb-stats-jsdeps.min.js'/ > "$i.html"
done

if [ -e pack.zip ]; then
	rm pack.zip
fi

zip -r pack.zip src/main/resources/ target/scala-2.11/zbdb-stats-{opt.js{,.map},jsdeps.min.js,launcher.js} index.css $(printf "%s.html\n" "${YEARS[@]}") $(printf "zbdb-%s-simplified.csv\n" "${YEARS[@]}")
