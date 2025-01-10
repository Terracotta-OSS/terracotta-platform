/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.api.model;

import org.terracotta.inet.HostPort;

import java.net.InetSocketAddress;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public final class Endpoints extends AbstractCollection<Node.Endpoint> {

  private final EndpointType endpointType;
  private volatile Cluster cluster;

  public Endpoints(Cluster cluster, EndpointType endpointType) {
    this.cluster = requireNonNull(cluster);
    this.endpointType = requireNonNull(endpointType);
  }

  public Cluster getCluster() {
    return cluster;
  }

  public EndpointType getEndpointType() {
    return endpointType;
  }

  @Override
  public Iterator<Node.Endpoint> iterator() {
    return endpoints().iterator();
  }

  public Iterator<Node.Endpoint> iterator(UID stripeUID) {
    final Cluster c = this.cluster;
    return c.getStripe(stripeUID)
        .map(stripe -> endpoints().filter(endpoint -> stripe.containsNode(endpoint.getNodeUID())))
        .orElse(Stream.empty())
        .iterator();
  }

  public Stream<Node.Endpoint> endpoints() {
    return cluster.getNodes().stream().map(node -> node.determineEndpoint(endpointType));
  }

  public Stream<Node.Endpoint> endpoints(UID stripeUID) {
    return cluster.getStripe(stripeUID).map(s -> s.getNodes().stream().map(node -> node.determineEndpoint(endpointType))).orElse(Stream.empty());
  }

  @Override
  public int size() {
    return cluster.getNodeCount();
  }

  public int size(UID stripeUID) {
    return cluster.getStripe(stripeUID).map(Stripe::getNodeCount).orElse(0);
  }

  public void refresh(Cluster cluster) {
    this.cluster = requireNonNull(cluster);
  }

  public Iterable<InetSocketAddress> asInetSocketAddresses() {
    return () -> endpoints().map(Node.Endpoint::getHostPort).map(HostPort::createInetSocketAddress).iterator();
  }

  public Iterable<InetSocketAddress> asInetSocketAddresses(UID stripeUID) {
    return () -> endpoints(stripeUID).map(Node.Endpoint::getHostPort).map(HostPort::createInetSocketAddress).iterator();
  }

  public Iterable<HostPort> asHostPorts() {
    return () -> endpoints().map(Node.Endpoint::getHostPort).iterator();
  }

  public Iterable<HostPort> asHostPorts(UID stripeUID) {
    return () -> endpoints(stripeUID).map(Node.Endpoint::getHostPort).iterator();
  }
}
