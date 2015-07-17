javac -sourcepath src -classpath classes/;lib/* -d classes/ src/snagentj/*.java src/snagentj/echodemo/*.java src/nanomsg/*.java || exit 1

echo "snagentj compiled successfully"