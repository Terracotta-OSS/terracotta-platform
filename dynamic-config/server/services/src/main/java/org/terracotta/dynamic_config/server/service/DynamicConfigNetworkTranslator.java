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
package org.terracotta.dynamic_config.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.inet.InetSocketAddressConverter;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * This service implements a network translator to support TC deployment behind a NAT-ed network for example.
 * <p>
 * If TC nodes are deployed in a separate network, recommendation is to configure public hostname/port on the
 * nodes and have all clients reach the nodes by using only public endpoints. The user will be responsible to
 * setup his networks (NAT, DNS ,etc) accordingly so that public endpoints can resolve to the nodes whether the
 * clients are "inside" or "outside" of the TC node network.
 * <p>
 * This is not possible to correctly support a mix of clients connecting to the cluster, some with public
 * endpoints and some with internal endpoints, because the NetworkTranslator has no way to know in a reliable
 * way which endpoint was used by the user in the URI to initiate the connection.
 *
 * @author Mathieu Carbou
 */
class DynamicConfigNetworkTranslator implements com.tc.spi.NetworkTranslator {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigNetworkTranslator.class);

  private final TopologyService topologyService;

  public DynamicConfigNetworkTranslator(TopologyService topologyService) {
    this.topologyService = topologyService;
  }

  /**
   * This implementation will return the right endpoint to use when a passive node is redirecting a client to an active node.
   * <p>
   * If the cluster is configured with public addresses, the public endpoint of the active server will be used.
   * <p>
   * If the cluster is not configured with public addresses, the internal endpoint will be used (which will be the parameter passed).
   * <p>
   * If the serverHostPort parameter passed does not allow us to correctly find the active node in the runtime topology, the parameter is returned.
   * But this situation should not happen because a removal of an active node is not permitted. So when a client connects, the active should be there.
   *
   * @param initiator      The remote address of the client
   * @param serverHostPort The internal address of the current active server
   * @return the endpoint to use to connect to the active server
   */
  @Override
  public String redirectTo(InetSocketAddress initiator, String serverHostPort) {
    final Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();
    InetSocketAddress proposedRedirect;
    try {
      proposedRedirect = InetSocketAddressConverter.getInetSocketAddress(serverHostPort);
    } catch (IllegalArgumentException e) {
      // Workaround because core does not correctly support / adhere to the new Ipv6 format for InetSocketAddresses (https://bugs.openjdk.java.net/browse/JDK-8232002)
      // In core, the same "hack" is used to extract the port when we know that a string contains a concatenation of ip:port
      int column = serverHostPort.lastIndexOf(':');
      if (column == -1) {
        throw e;
      }
      proposedRedirect = InetSocketAddress.createUnresolved(serverHostPort.substring(0, column), Integer.parseInt(serverHostPort.substring(column + 1)));
    }
    Optional<Node.Endpoint> publicEndpoint = cluster.findReachableNode(proposedRedirect).flatMap(Node::getPublicEndpoint);
    if (publicEndpoint.isPresent()) {
      LOGGER.trace("Redirecting client: {} to node: {} through public endpoint", initiator, publicEndpoint.get());
      return publicEndpoint.get().getAddress().toString();
    } else {
      // we were not able to find the serverHostPort in the topology.
      LOGGER.trace("Redirecting client: {} to proposed address: {}", initiator, serverHostPort);
      return serverHostPort;
    }
  }
}
