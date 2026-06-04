@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
set "M2_HOME=%~dp0.mvn\apache-maven-3.9.16"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"
call "%M2_HOME%\bin\mvn.cmd" %*
