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
TC_VOTER_MAIN=org.terracotta.voter.cli.TCVoterMain

# this will only happen if using sag installer
if [ -r "${TC_VOTER_DIR}/bin/setenv.sh" ] ; then
  . "${TC_VOTER_DIR}/bin/setenv.sh"
fi

if ! [ -d "${JAVA_HOME}" ]; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

TC_KIT_ROOT="$(dirname "$(dirname "$TC_VOTER_DIR")")"
TC_LOGGING_ROOT="$TC_KIT_ROOT/client/logging"
TC_CLIENT_ROOT="$TC_KIT_ROOT/client/lib"

CLASS_PATH="${TC_VOTER_DIR}/lib/*:${TC_CLIENT_ROOT}/*:${TC_LOGGING_ROOT}/*:${TC_LOGGING_ROOT}/impl/*:${TC_LOGGING_ROOT}/impl/"

"$JAVA_HOME/bin/java" ${JAVA_OPTS} -cp "$CLASS_PATH" $TC_VOTER_MAIN "$@"
