#!/bin/bash
path=$(cd ..; pwd)

javac -d "$(pwd)" \
  "$path"/java-solutions/info/kgeorgiy/ja/sitkina/bank/BankTests.java\
  -cp "$path/java-solutions:$path/lib/*"
java -cp ".:$path/lib/*" info.kgeorgiy.ja.sitkina.bank.BankTests
exitcode=$?
rm -r info
exit $exitcode