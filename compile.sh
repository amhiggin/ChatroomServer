#!/bin/sh
#set -x

###########################
## Shell script to compile the 
## https://www.github.com/amhiggin/ChatroomServer java project.
## Assumed Java and Git installed on host machine.
###########################


echo "Running compilation on Chatroom Server project"

# Compile with dependencies
javac -cp lib/* src/main/java/*.java

echo "Done."
