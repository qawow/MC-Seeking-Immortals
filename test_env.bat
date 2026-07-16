@echo off
REM Linux equivalent: ./test_env.sh
echo === Testing Java ===
java -version
echo.
echo === Testing Gradle ===
call gradlew.bat --version
