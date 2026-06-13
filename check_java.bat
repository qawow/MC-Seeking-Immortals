@echo off
echo Checking Java installation...
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found in PATH
    echo Please install JDK 17 from: https://adoptium.net/temurin/releases/?version=17
    echo After installation, add JAVA_HOME to environment variables
    exit /b 1
)

java -version
echo.
echo Java found! Checking Gradle wrapper...
gradlew.bat --version
