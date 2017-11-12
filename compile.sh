#!/bin/sh
#set -x

###########################
## Shell script to compile the 
## https://www.github.com/amhiggin/ChatroomServer java project.
## Assumed Java and Git installed on host machine.
###########################


echo "Running compilation on Chatroom Server project"

# Compile with dependencies
javac -cp lib/joda-time-2.9.9.jar:lib/commons-io-2.6.jar src/main/java/*.java

echo "Done."
