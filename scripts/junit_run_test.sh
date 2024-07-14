#!/bin/bash
path=$(cd ..; pwd)

javac -d "$(pwd)" \
  "$path"/java-solutions/info/kgeorgiy/ja/sitkina/bank/BankTests.java\
  -cp "$path/java-solutions:$path/lib/*"
java -jar ../lib/junit-platform-console-standalone-1.9.3.jar -cp ".:$path/lib/*" --scan-class-path
exitcode=$?
rm -r info
exit $exitcode