#!/bin/bash

CP=.
CP=${CP}:target/test-classes
CP=${CP}:target/classes

CLASS=com.bpcreates.remoteclient.RemoteClientTest

HOST='bpcreates.com'
PORT='10005'
TYPES=(channel socket)
ARGS=('READY' 'ping' 'PING' 'echo:harry' '<policy-file-request/>' 'asdfasdfasdf' '')

for type in ${TYPES[*]} 
do
    for arg in ${ARGS[*]}
    do
	echo "running test of ${type} using ${arg}"
	java -cp ${CP} ${CLASS} "${type}" "${arg}" "${HOST}" "${PORT}"
    done
done

#java -cp ${CP} channel stop

