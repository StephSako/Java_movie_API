#!/bin/sh
clear && javac -Xlint:unchecked projet_GLPOO_3A/Main.java && java -cp .:../bdd/sqlite-jdbc-3.27.2.jar projet_GLPOO_3A.Main $1 | jq .
