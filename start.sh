#!/bin/sh
#set -x

###########################
## Shell script to run the compiled chat server from 
## https://www.github.com/amhiggin/ChatroomServer github repository.
## Requires running compile.sh first, and then running script with argument $1 portNumber
###########################

echo "Starting up chatroom server on port $1"

java -showversion -cp lib/*:src/main/java/* src main.java.ChatroomServer $1
