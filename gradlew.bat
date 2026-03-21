@if echo off
echo This is a Gradle wrapper batch file for Windows systems.
rem This script would normally execute Gradle, but for our purposes
rem we'll just make sure it exists as part of the standard Android project structure
java -jar "%~dp0gradle\wrapper\gradle-wrapper.jar" %*