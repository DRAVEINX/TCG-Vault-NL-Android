@echo off
setlocal
if exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
  java -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) else (
  where gradle >nul 2>&1
  if errorlevel 1 (
    echo Gradle-wrapper.jar ontbreekt. Installeer Gradle 8.11.1 en voer gradle wrapper --gradle-version 8.11.1 uit.
    exit /b 1
  )
  gradle %*
)
