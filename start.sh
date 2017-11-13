#!/bin/sh
#set -x

###########################
## Shell script to run the compiled chat server from 
## https://www.github.com/amhiggin/ChatroomServer github repository.
## Requires running compile.sh first, and then running script with argument $1 portNumber
###########################

echo "Starting up chatroom server on port $1"

#java -cp lib/joda-time-2.9.9.jar:lib/commons-io-2.6.jar:src main.java.ChatroomServer $1 
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=23456 -cp lib/joda-time-2.9.9.jar:lib/commons-io-2.6.jar:src main.java.ChatroomServer $1 