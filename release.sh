#!/bin/sh

# overview:
#  - before running this script, follow steps 1-3.
#  - running this script counts as step 4.
#  - after running this script, follow step 5.
#
# step 1: double-check scala.js and scala native versions.
#
# step 2: tag the release
#
# step 3: edit build.sbt
#    a. set versionNumber to this release number (e.g. "1.14.2" or "1.14.3-M1")
#       this version could include RC or M but should not include SNAPSHOT.
#
# step 4: start the actual release
#    a. run ./release.sh publishSigned
#    b. you need to have PGP set up to work with SBT.
#       we'll invoke SBT 5 times (jvm + 2js + 2native)
#       so you'll need to enter your password 5 times.
#       it's annoying :/
#
# step 5: edit build.sbt again
#    a. set versionNumber to the next release number (e.g. "1.14.3")
#       note -- don't include SNAPSHOT, RC, or M in this version.

usage() {
    echo "usage: $0 [compile | test | package | publishLocal | publishSigned]"
    exit 1
}

runsbt() {
    IS_RELEASE="true" sbt "$1"
    RES=$?
    if [ $RES -ne 0 ]; then
        echo "sbt '$1' failed: $RES"
        exit $RES
    fi
}

if [ $# -ne 1 ]; then usage; fi

case "$1" in
    compile|test|package|publishLocal|publishSigned)
        CMD="$1";;
    *) echo "unknown argument: $1"; usage;;
esac

# first we clean everything to avoid stale artifacts.
runsbt "+ clean"

# step 4a: jvm release
runsbt "+ jvm/$CMD"

# step 4b: js releases
runsbt "+ js/$CMD"

# step 4c: native releases
runsbt "+ native/$CMD"
