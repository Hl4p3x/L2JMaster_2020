#!/bin/bash

err=1

ASIGNED_MEMORY=256m

until [ $err == 0 ]; 
do
	[ -f log/java0.log.0 ] && mv log/java0.log.0 "log/`date +%Y-%m-%d_%H-%M-%S`_java.log"
	[ -f log/stdout.log ] && mv log/stdout.log "log/`date +%Y-%m-%d_%H-%M-%S`_stdout.log"
	java -Xms128m -Xmx$ASIGNED_MEMORY -jar login.jar > log/stdout.log 2>&1
	err=$?
	sleep 10;
done
