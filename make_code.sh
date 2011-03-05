#!/bin/sh
echo [[[ MAKING COMMON CODE ]]
javac common/*.java
echo [[[ MAKING CLIENT CODE ]]
javac client/*.java
echo [[[ MAKING SERVER CODE ]]
javac server/*.java
