#!/bin/sh
# Bind Tomcat to the port Railway provides (defaults to 8080 locally).
set -eu

PORT_VALUE="${PORT:-8080}"

if [ -f /usr/local/tomcat/conf/server.xml ]; then
  sed -i "s/8080/${PORT_VALUE}/g" /usr/local/tomcat/conf/server.xml
fi

exec /usr/local/tomcat/bin/catalina.sh run
