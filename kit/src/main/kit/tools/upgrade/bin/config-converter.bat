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
setlocal EnableExtensions EnableDelayedExpansion

pushd "%~dp0.."
set "CONFIG_CONVERTER_TOOL_DIR=%CD%"
popd

if exist "!CONFIG_CONVERTER_TOOL_DIR!\bin\setenv.bat" (
  pushd "!CONFIG_CONVERTER_TOOL_DIR!\bin" && (
    call .\setenv.bat
    popd
  )
)

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set "JAVA=%JAVA_HOME%\bin\java.exe"
set java_opts=%JAVA_OPTS%
set "CP=%CONFIG_CONVERTER_TOOL_DIR%\lib\*;%CONFIG_CONVERTER_TOOL_DIR%\..\..\server\lib\*;%CONFIG_CONVERTER_TOOL_DIR%\..\..\server\plugins\api\*;%CONFIG_CONVERTER_TOOL_DIR%\..\..\server\plugins\lib\*"
"%JAVA%" %java_opts% -cp "%CP%" org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigConverterTool %*

exit /b %ERRORLEVEL%
endlocal