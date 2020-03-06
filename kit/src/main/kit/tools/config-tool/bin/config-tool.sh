#!/bin/sh
#
# Copyright Terracotta, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

CONFIG_TOOL_DIR=$(dirname "$(cd "$(dirname "$0")";pwd)")

# this will only happen if using sag installer
if [ -r "${CONFIG_TOOL_DIR}/bin/setenv.sh" ] ; then
  . "${CONFIG_TOOL_DIR}/bin/setenv.sh"
fi

java_opts="$JAVA_OPTS"

if [ ! -d "$JAVA_HOME" ]; then
   echo "ERROR: JAVA_HOME must point to Java installation."
   echo "    $JAVA_HOME"
   exit 2
fi

JAVA="$JAVA_HOME/bin/java"

"$JAVA" $java_opts -cp "$CONFIG_TOOL_DIR/lib/*:$CONFIG_TOOL_DIR/../common/lib/*" org.terracotta.dynamic_config.cli.config_tool.ConfigTool "$@"