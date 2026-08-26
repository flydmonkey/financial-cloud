@echo off
echo set env

set JBX_VERSION=1.1.0
set JBX_REPOSITORY=jinbooks

if "%JAVA_HOME%"=="" (
    echo JAVA_HOME is not set. Please install JDK 17+ and set JAVA_HOME.
    exit /b 1
)

call "%JAVA_HOME%\bin\java" -version
call mvnw.cmd -version
