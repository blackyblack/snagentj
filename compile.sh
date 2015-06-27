#!/bin/sh
CP=classes/:lib/*
SP=src/

/bin/rm -rf classes
/bin/mkdir -p classes/

javac -sourcepath ${SP} -classpath ${CP} -d classes/ src/snagentj/*.java src/nanomsg/*.java src/nanomsg/*/*.java || exit 1

echo "snagentj compiled successfully"