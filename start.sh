#!/bin/sh
#set -x

###########################
## Shell script to run the compiled chat server from 
## https://www.github.com/amhiggin/ChatroomServer github repository.
## Requires running compile.sh first, and then running script with argument $1 portNumber
###########################

echo "Starting up chatroom server on port $1"

java -j:-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=12345 -cp lib/*:src main.java.ChatroomServer $1 
