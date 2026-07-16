@echo off
REM Linux equivalent: ./set_java_home.sh
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
echo JAVA_HOME has been set to: C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot
echo.
echo Please RESTART your terminal (close and reopen), then run:
echo   gradlew.bat compileJava
pause
