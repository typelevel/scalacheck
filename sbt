#!/bin/sh

java -Xmx1024M -jar `dirname $0`/sbt-launcher.jar "$@"
