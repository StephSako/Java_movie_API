#!/bin/sh
clear && javac -Xlint:unchecked java_project/Main.java && java -cp .:../bdd/sqlite-jdbc-3.27.2.jar java_project.Main $1
