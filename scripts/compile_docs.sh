#!/bin/bash

class_name="../java-solutions/info/kgeorgiy/ja/sitkina/implementor/Implementor.java"
implementor_path="../../java-advanced-2024/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/"

SRC="${implementor_path}JarImpler.java
${implementor_path}Impler.java
${implementor_path}ImplerException.java"

javadoc -d docs -private $class_name $SRC
