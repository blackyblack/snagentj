#!/bin/sh
CP=classes/:lib/*
SP=src/

/bin/rm -rf classes
/bin/mkdir -p classes/

javac -sourcepath ${SP} -classpath ${CP} -d classes/ src/agent1/*.java src/nanomsg/*.java src/nanomsg/*/*.java || exit 1

echo "agent1 class files compiled successfully"