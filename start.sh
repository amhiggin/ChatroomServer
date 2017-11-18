#!/bin/sh
#set -x

###########################
## Shell script to run the compiled chat server from 
## https://www.github.com/amhiggin/ChatroomServer github repository.
## Requires running compile.sh first, and then running script with argument $1 portNumber
## NOTE: do not uncomment both lines (1) and (2) at the same time. (2) will suspend the server until a debugger attaches.
###########################

echo "Starting up chatroom server on port $1"

## (1) This line will launch the server, passing arg $1 as the socket.
java -cp lib/joda-time-2.9.9.jar:lib/commons-io-2.6.jar:src main.java.ChatroomServer $1 

## (2) This line will launch the server with remote debugging available at port 23456.
#java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=23456 -cp lib/joda-time-2.9.9.jar:lib/commons-io-2.6.jar:src main.java.ChatroomServer $1 