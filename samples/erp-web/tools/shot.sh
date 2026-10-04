#!/bin/sh
# A picture of the web version, painted rather than photographed.
#
#     sh tools/build.sh
#     sh tools/shot.sh [target.png [width height]]
set -e
cd "$(dirname "$0")/.."
JAR=build/RapidXERP.jar
if [ ! -f "$JAR" ]; then
    echo "build/RapidXERP.jar is missing - run  sh tools/build.sh  first." >&2
    exit 1
fi
CP="$JAR:build/lib/*"
javac -encoding UTF-8 -cp "$CP" -d tools/build tools/Shot.java
DB=$(mktemp -d)/shot.db
RAPIDXERP_DB="$DB" java -cp "$CP:tools/build" Shot "$@"
