@echo on

cd /d %~dp0
 
call env.cmd

echo JAVA_HOME = %JAVA_HOME%
echo CVSROOT = %CVSROOT%

if "%CVSROOT%" == "" goto error

if "%JAVA_HOME%" == "" goto error

"%JAVA_HOME%\bin\java" -Dcvs.root=%CVSROOT% -Dfile.encoding=UTF8 -Dclient.encoding.override=UTF-8 -jar clobcopy-1.0-jar-with-dependencies.jar cfgFile=clobcopy.properties

goto exit_pr

:error
echo error
echo please check CVSROOT
echo please check JAVA_HOME


:exit_pr
echo Ok