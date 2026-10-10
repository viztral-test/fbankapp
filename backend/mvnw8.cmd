@echo off
rem Runs the Maven Wrapper on JDK 8 without touching the system-wide JAVA_HOME.
rem Override the JDK location by setting FBANKAPP_JDK8_HOME before calling this script.
setlocal
if not defined FBANKAPP_JDK8_HOME set "FBANKAPP_JDK8_HOME=%USERPROFILE%\.jdks\jdk8u504-b01"
if not exist "%FBANKAPP_JDK8_HOME%\bin\java.exe" (
    echo ERROR: JDK 8 not found at "%FBANKAPP_JDK8_HOME%". 1>&2
    echo Set FBANKAPP_JDK8_HOME to a JDK 8 installation directory. 1>&2
    exit /b 1
)
set "JAVA_HOME=%FBANKAPP_JDK8_HOME%"
call "%~dp0mvnw.cmd" %*
exit /b %ERRORLEVEL%
