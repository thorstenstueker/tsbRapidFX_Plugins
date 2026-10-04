#!/bin/sh
# Check the designer round trip, then compile.
#
#     sh tools/build.sh              check and compile
#     sh tools/build.sh --quick      compile only, for a change that touched no screens
#
# THE SOURCE IS src/. THERE IS NOTHING THAT GENERATES IT.
#
# It used to be otherwise: tools/GenerateForms.java wrote every screen from scratch, taking the
# half above the DO-NOT-EDIT banner out of tools/handwritten/*.txt. That made this script
# dangerous in a way nothing announced — open a screen in the designer, change it, run this,
# and the change was replaced by the old copy without a word. The round trip below could not
# notice, because it compared the freshly written file with itself.
#
# Both are gone. Every screen in src/ is an ordinary designer form: open it, change it, save
# it. That is also what this project is meant to demonstrate, and a demonstration you may only
# half open is the wrong one.
#
# What the round trip does is read every screen with the designer's own parser, write it out
# again and compare. If the same file comes back, the designer can open it and a save changes
# no line. It writes nothing to disk — it compares in memory.
set -e

# The compiler. The default is where it lies in this repository; RFXC=... overrides it.
RFXC=${RFXC:-../../compiler/cli/build/libs/rfxc-0.1.0.jar}
DESIGNER=tools/tsbdesignerswx.jar

cd "$(dirname "$0")/.."

if [ "$1" != "--quick" ]; then
    echo "== designer round trip =="
    javac -encoding UTF-8 -cp "$DESIGNER" -d tools/build tools/CheckForms.java
    java -cp "$DESIGNER:tools/build" CheckForms src
    echo
fi

echo "== compiling =="
java -jar "$RFXC" --project RapidXERP.rfxproj
