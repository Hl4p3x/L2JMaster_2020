#!/bin/sh
java -Djava.util.logging.config.file=console.cfg -cp ./../libs/*:login.jar com.l2jserver.tools.accountmanager.SQLAccountManager
