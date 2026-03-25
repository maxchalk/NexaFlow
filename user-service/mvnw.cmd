@echo off
setlocal
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6
if not exist "%MAVEN_HOME%" (
    mkdir "%USERPROFILE%\.m2\wrapper\dists" 2>nul
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%USERPROFILE%\.m2\wrapper\dists\maven.zip'"
    powershell -Command "Expand-Archive -Path '%USERPROFILE%\.m2\wrapper\dists\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
    del "%USERPROFILE%\.m2\wrapper\dists\maven.zip"
)
"%MAVEN_HOME%\bin\mvn.cmd" %*
