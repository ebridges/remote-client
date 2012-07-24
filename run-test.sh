#!/bin/bash

CP=.
CP=${CP}:target/test-classes
CP=${CP}:target/classes

CLASS=com.bpcreates.remoteclient.RemoteClientTest

TYPES=(channel socket)
ARGS=('echo:harry' '<policy-file-request/>' 'asdfasdfasdf')

for type in ${TYPES[*]} 
do
    for arg in ${ARGS[*]}
    do
	echo "running test of ${type} using ${arg}"
	java -cp ${CP} ${CLASS} "${type}" "${arg}"
    done
done

#java -cp ${CP} channel stop

