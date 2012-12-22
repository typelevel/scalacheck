#!/bin/sh

java -Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=384M -jar `dirname $0`/tools/sbt-launch-0.12.1.jar "$@"
