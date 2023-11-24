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

public interface TCVoter {

  /**
   * Register this voter instance with a cluster.
   *
   * @param clusterName an identifier for the target cluster
   * @param hostPorts   the host:port combination of all the servers in that stripe
   * @return A Future that holds the registration status of the voter.
   * This Future will complete once the registration succeeds.
   */
  VoterStatus register(String clusterName, String... hostPorts);

  /**
   * Send an override vote to the server at the given host:port to force promote it to be an active.
   * The server will accept this vote if and only if it's trying to get promoted.
   * Else this vote will be ignored.
   *
   * @param hostPort The host:port combination of the target server
   * @return true if the override vote was accepted by the server. Else false.
   */
  boolean overrideVote(String hostPort);

  /**
   * De-register this voter instance from a cluster that it was previously registered with.
   *
   * @param clusterName the cluster name that was used to register this voter with a cluster
   */
  void deregister(String clusterName);

}
