#!/bin/sh
DIR="$( dirname $0 )"
java -cp $DIR/classes:$DIR/lib:$DIR/lib/*:$DIR/lib/linux-x86-64/libnanomsg.so:$DIR/linux-x86-64/libprocutils.so snagentj.echodemo.Application "$@"