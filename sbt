#!/bin/sh

java -Xmx1024M -jar `dirname $0`/lib/sbt-launcher.jar "$@"
