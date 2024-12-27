@echo off

cd /D %~dp0

start "assBrowser" javaw -classpath "%~dp0\bin" -Xms16m -Xmx1024m com.asofterspace.assBrowser.AssBrowser %*

exit
