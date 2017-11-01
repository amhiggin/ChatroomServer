#!/bin/sh
#set -x

###########################
## Shell script to run the compiled chat server from 
## https://www.github.com/amhiggin/ChatroomServer github repository.
## Requires running compile.sh first, and then running script with argument $1 portNumber
###########################


JAVAHOME=/usr/local/java/jdk1.8.0_101/

portNumber = $1


$JAVAHOME/bin/java -showversion -cp  $CLASSPATH\*  main.java.ChatroomServer ${port_number}

echo "Starting up chatroom server on port ${portNumber}"
