#!/bin/sh

java -Xmx1024M -jar `dirname $0`/tools/sbt-launch-0.7.4.jar "$@"
