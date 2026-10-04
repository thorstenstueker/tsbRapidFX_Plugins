#!/bin/sh
# A picture of the running program, painted rather than photographed.
#
#     sh tools/build.sh          # first, so that build/ is up to date
#     sh tools/shot.sh [target.png]
#
# The database is a fresh file in a temporary folder, exactly as the self test does it: the
# picture then shows the sample data every first start produces, and data/rapidxerp.db is
# never opened.
set -e

cd "$(dirname "$0")/.."

JAR=build/RapidXERPDesktop.jar
if [ ! -f "$JAR" ]; then
    echo "build/RapidXERPDesktop.jar is missing - run  sh tools/build.sh  first." >&2
    exit 1
fi

CP="$JAR:build/lib/*"

javac -encoding UTF-8 -cp "$CP" -d tools/build tools/Shot.java

DB=$(mktemp -d)/shot.db
RAPIDXERP_DB="$DB" java -cp "$CP:tools/build" Shot "$@"
