#!/bin/bash

class_name="info/kgeorgiy/ja/sitkina/implementor/Implementor"
temp_dir="temp"
kgeorgiy_implementor_path="../../java-advanced-2024/modules/info.kgeorgiy.java.advanced.implementor"
class_path="../java-solutions/${class_name}.java"

javac -d "$temp_dir" -cp "$kgeorgiy_implementor_path" "$class_path"
jar cfm implementor.jar MANIFEST.MF -C "$temp_dir" .
rm -r "$temp_dir"