#!/bin/sh

java $SBT_OPTS -Xmx1024M -XX:MaxPermSize=512m -jar `dirname $0`/tools/sbt-launch-0.10.1.jar "$@"
