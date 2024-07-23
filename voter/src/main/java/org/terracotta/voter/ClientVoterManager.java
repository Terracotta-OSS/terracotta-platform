/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.connection.ConnectionException;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public interface ClientVoterManager {

  /**
   * The host and port information of the server that this client is connected to.
   *
   * @return host and port of the server separated by a ":"
   */
  String getTargetHostPort();

  /**
   * Establish a connection with the server at the given host and port
   */
  void connect(Properties connectionProps) throws ConnectionException;

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
  
  boolean isRegistered();
  
  long generation();
  
  long lastVotedGeneration();
  
  void zombie();

  boolean isConnected();
  
  boolean register(String id) throws TimeoutException;

  /**
   * @return the number of voters that have been registered with the server.
   */
  long getRegisteredVoterCount() throws TimeoutException;

  /**
   * @return the maximum number of voters that can be registered with the server.
   */
  long getRegisteredVoterLimit() throws TimeoutException;


  long HEARTBEAT_RESPONSE = 0;
  long INVALID_VOTER_RESPONSE = -1;

  /**
   *
   * @param id voter id
   * @return a positive election term number when the server is in election.
   * 0 if the server is not in election. -1 if the server does not recognise this voter as a valid one.
   */
  long heartbeat(String id) throws TimeoutException;

  /**
   *
   * @param id the voter id
   * @return a positive election term number when the server is in election.
   * 0 if the server is not in election. -1 if the server does not recognise this voter as a valid one.
   */
  long vote(String id) throws TimeoutException;

  /**
   * For casting an override vote during election.
   * An override vote is accepted by the server if and only if the server is in the middle of an election.
   * Override votes are ignored if the vote is cast when the server is not in election.
   *
   * @param id the voter id
   */
  boolean overrideVote(String id) throws TimeoutException;

  /**
   * De-register the voter with the given id from the server.
   *
   * @param id the voter id
   * @return true if de-registration succeeds, otherwise false.
   */
  boolean deregisterVoter(String id) throws TimeoutException;
}
