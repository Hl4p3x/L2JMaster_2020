#!/bin/bash

# exit codes of GameServer:
#  0 normal shutdown
#  2 reboot attempt

ASIGNED_MEMORY=6144m

while :; do
	[ -f log/java0.log.0 ] && mv log/java0.log.0 "log/`date +%Y-%m-%d_%H-%M-%S`_java.log"
	[ -f log/stdout.log ] && mv log/stdout.log "log/`date +%Y-%m-%d_%H-%M-%S`_stdout.log"
	java -Xms3072m -Xmx$ASIGNED_MEMORY -jar l2jserver.jar > log/stdout.log 2>&1
	[ $? -ne 2 ] && break
	sleep 10
done
