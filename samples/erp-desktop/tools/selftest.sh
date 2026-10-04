#!/bin/sh
# One round through the whole program, on a database of its own.
#
#     sh tools/build.sh          # first, so that build/ is up to date
#     sh tools/selftest.sh
#
# The database is a fresh file in a temporary folder, named by RAPIDXERP_DB, which is the
# environment variable Database.FilePath looks for. The real data/rapidxerp.db is never opened,
# so a failed run cannot leave rubbish in it and a passing run proves the first start works.
set -e

cd "$(dirname "$0")/.."

JAR=build/RapidXERPDesktop.jar
if [ ! -f "$JAR" ]; then
    echo "build/RapidXERPDesktop.jar is missing - run  sh tools/build.sh  first." >&2
    exit 1
fi

CP="$JAR:build/lib/*"

echo "== compiling the test =="
javac -encoding UTF-8 -cp "$CP" -d tools/build tools/SelfTest.java

echo
echo "== running it =="
DB=$(mktemp -d)/selftest.db
RAPIDXERP_DB="$DB" java -cp "$CP:tools/build" SelfTest
