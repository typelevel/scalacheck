@echo off
set SCRIPT_DIR=%~dp0
if defined PROXY_HOST set PROXY_OPTS=-Dhttp.proxyHost=%PROXY_HOST% -Dhttp.proxyPort=%PROXY_PORT% -Dhttp.proxyUser=%PROXY_USER% -Dhttp.proxyPassword=%PROXY_PASSWORD%
java %SBT_OPTS% %PROXY_OPTS% -Xmx1024M -jar "%SCRIPT_DIR%/tools/sbt-launch-0.11.3.jar" %*
