@echo off
rem Starts the whole fbankapp demo: backend on http://localhost:8080 and frontend on http://localhost:4200.
rem Each one opens in its own window; close a window (or press Ctrl+C in it) to stop that part.
rem Needs JDK 8 (see README.md, FBANKAPP_JDK8_HOME) and Node.js with npm.
setlocal

rem Backend: Spring Boot on JDK 8 through the Maven Wrapper
start "fbankapp backend" /d "%~dp0backend" cmd /k "mvnw8.cmd spring-boot:run"

rem Frontend: install packages on the first run only, then start the Angular dev server
start "fbankapp frontend" /d "%~dp0frontend" cmd /k "(if not exist node_modules npm install) && npm start"

echo Backend  : http://localhost:8080  (first start takes a little while)
echo Frontend : http://localhost:4200
echo Demo login (local testing only): demo / Demo@1234
endlocal
