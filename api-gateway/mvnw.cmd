@echo off
setlocal
set MAVEN_PROJECTBASEDIR=%~dp0
for /f "tokens=2 delims==" %%G in ('findstr "distributionUrl" "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"') do set DISTRIBUTION_URL=%%G
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6
if not exist "%MAVEN_HOME%" (
    mkdir "%USERPROFILE%\.m2\wrapper\dists"
    powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%USERPROFILE%\.m2\wrapper\dists\maven.zip'"
    powershell -Command "Expand-Archive -Path '%USERPROFILE%\.m2\wrapper\dists\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists'"
    del "%USERPROFILE%\.m2\wrapper\dists\maven.zip"
)
set MVN_BIN=%MAVEN_HOME%\bin\mvn.cmd
if not exist "%MVN_BIN%" set MVN_BIN=%MAVEN_HOME%\bin\mvn
"%MVN_BIN%" %*
