/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.voter;

import com.tc.voter.VoterManager;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public interface ClientVoterManager extends VoterManager {

  /**
   * The host and port information of the server that this client is connected to.
   *
   * @return host and port of the server separated by a ":"
   */
  String getTargetHostPort();

  /**
   * Establish a connection with the server at the given host and port
   */
  void connect(Optional<Properties> connectionProps);

  /**
   * @return the current state of the server that this voter is connected to.
   */
  String getServerState() throws TimeoutException;

  /**
   * @return the configuration of the server that this voter is connected to.
   * @throws TimeoutException
   */
  String getServerConfig() throws TimeoutException;

  Set<String> getTopology() throws TimeoutException;

  /**
   * Close the connection with the server.
   */
  void close();

  boolean isVoting();

  void zombie();

  boolean isConnected();

  /**
   * @return the number of voters that have been registered with the server.
   */
  long getRegisteredVoterCount() throws TimeoutException;

  /**
   * @return the maximum number of voters that can be registered with the server.
   */
  long getRegisteredVoterLimit() throws TimeoutException;
}
