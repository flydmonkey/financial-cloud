@echo off
call setEnvVars.bat

set START_TIME=%date:~0,10% %time:~0,8%
echo start time %START_TIME%

echo start clean . . .
call mvnw.cmd clean -q

echo clean complete .

call mvnw.cmd -DskipTests package

set END_TIME=%date:~0,10% %time:~0,8%
echo Build Release start at %START_TIME% complete at %END_TIME%.

pause
