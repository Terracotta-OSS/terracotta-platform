@REM
@REM Copyright Terracotta, Inc.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@echo off
setlocal enabledelayedexpansion enableextensions

pushd "%~dp0.."
set "TC_SERVER_DIR=%CD%"
popd
set "PLUGIN_LIB_DIR=%TC_SERVER_DIR%\plugins\lib"
set "PLUGIN_API_DIR=%TC_SERVER_DIR%\plugins\api"

if exist "!TC_SERVER_DIR!\bin\setenv.bat" (
  pushd "!TC_SERVER_DIR!\bin" && (
    call .\setenv.bat
    popd
  )
)

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

for %%C in ("-d64 -server -XX:MaxDirectMemorySize=1048576g" ^
			"-server -XX:MaxDirectMemorySize=1048576g" ^
			"-d64 -client  -XX:MaxDirectMemorySize=1048576g" ^
			"-client -XX:MaxDirectMemorySize=1048576g" ^
			"-XX:MaxDirectMemorySize=1048576g") ^
do (
  set JAVA_COMMAND="%JAVA_HOME%\bin\java" %%~C
  !JAVA_COMMAND! -version > NUL

  if not errorlevel 1 (
	goto setJavaOptsAndClasspath
  ) else (
    echo [!JAVA_COMMAND!] failed - trying further options
  )
)
echo No executable Java environment found in [%JAVA_HOME%]
exit /b 1

:setJavaOptsAndClasspath

REM fixes bug when command length exceeds max windows command length of 8191
set "PLUGIN_CLASSPATH=%PLUGIN_LIB_DIR%\*;%PLUGIN_API_DIR%\*"

REM   Adding SLF4j libraries to the classpath of the server to
REM   support services that may use SLF4j for logging
if exist "!TC_SERVER_DIR!\lib" (
  pushd "!TC_SERVER_DIR!\lib" && (
    for %%K in ( slf4j*.jar ) do (
      set "PLUGIN_CLASSPATH=!PLUGIN_CLASSPATH!;!CD!\%%K"
    )
    popd
  )
) else (
  echo !TC_SERVER_DIR!\lib does not exist!
)

set "CLASSPATH=%TC_SERVER_DIR%\lib\tc.jar;%PLUGIN_CLASSPATH%;%TC_SERVER_DIR%\lib"
set OPTS=%SERVER_OPT% -Xms256m -Xmx2g -XX:+HeapDumpOnOutOfMemoryError
rem rmi.dgc.server.gcInterval is set as year to avoid system gc in case authentication is enabled
rem users may change it accordingly
set OPTS=%OPTS% "-Dtc.install-root=%TC_SERVER_DIR%"
set JAVA_OPTS=%OPTS% %JAVA_OPTS%

:START_NODE

%JAVA_COMMAND% %JAVA_OPTS% -cp "%CLASSPATH%" org.terracotta.dynamic_config.server.TerracottaNode %*
if %ERRORLEVEL% EQU 11 (
	echo start-node: Restarting the server...
	goto START_NODE
)
exit /b %ERRORLEVEL%
endlocal
