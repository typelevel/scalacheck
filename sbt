#!/bin/sh

java $SBT_OPTS -Xmx1024M -jar `dirname $0`/tools/sbt-launch-0.7.4.jar "$@"
