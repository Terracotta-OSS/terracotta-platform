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

TC_VOTER_DIR="$(dirname "$(cd "$(dirname "$0")";pwd)")"

# this will only happen if using sag installer
if [ -r "${TC_VOTER_DIR}/bin/setenv.sh" ] ; then
  . "${TC_VOTER_DIR}/bin/setenv.sh"
fi

if ! [ -d "${JAVA_HOME}" ]; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

CLASS_PATH="${TC_VOTER_DIR}/../../client/lib/*:${TC_VOTER_DIR}/lib/*:${TC_VOTER_DIR}/../lib/*"

"$JAVA_HOME/bin/java" ${JAVA_OPTS} "-Dlogback.configurationFile=logback-voter.xml" -cp "$CLASS_PATH" org.terracotta.voter.cli.TCVoterMain "$@"
