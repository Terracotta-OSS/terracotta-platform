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
package org.terracotta.dynamic_config.api.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


public class Stripe implements Cloneable, PropertyHolder {
  private final List<Node> nodes;

  public Stripe(List<Node> nodes) {
    this.nodes = new CopyOnWriteArrayList<>(requireNonNull(nodes));
  }

  public Stripe(Node... nodes) {
    this(Arrays.asList(nodes));
  }

  public Stripe() {
    this.nodes = new ArrayList<>();
  }

  public List<Node> getNodes() {
    return Collections.unmodifiableList(nodes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Stripe stripe = (Stripe) o;
    return nodes.equals(stripe.nodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodes);
  }

  @Override
  public String toString() {
    return "Stripe{" +
        "nodes=" + nodes +
        '}';
  }

  public String toShapeString() {
    return "( " + nodes.stream().map(node -> node.getNodeName() + "@" + node.getNodeAddress()).collect(joining(", ")) + " )";
  }

  public Collection<InetSocketAddress> getNodeAddresses() {
    return getNodes().stream().map(Node::getNodeAddress).collect(toList());
  }

  public Optional<Node> getSingleNode() throws IllegalStateException {
    if (nodes.size() > 1) {
      throw new IllegalStateException();
    }
    if (nodes.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(nodes.iterator().next());
  }

  public Optional<Node> getNode(InetSocketAddress address) {
    return nodes.stream().filter(node -> node.hasAddress(address)).findAny();
  }

  public boolean containsNode(InetSocketAddress address) {
    return nodes.stream().anyMatch(node -> node.hasAddress(address));
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Stripe clone() {
    return new Stripe(nodes.stream().map(Node::clone).collect(toList()));
  }

  public boolean removeNode(InetSocketAddress address) {
    return nodes.removeIf(node -> node.hasAddress(address));
  }

  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  public Stripe addNode(Node source) {
    nodes.add(source);
    return this;
  }

  public int getNodeCount() {
    return nodes.size();
  }

  public Optional<Node> getNode(String nodeName) {
    return nodes.stream().filter(node -> node.getNodeName().equals(nodeName)).findAny();
  }

  public Optional<Node> getNode(int nodeId) {
    if (nodeId < 1) {
      throw new IllegalArgumentException("Invalid node ID: " + nodeId);
    }
    if (nodeId > nodes.size()) {
      return Optional.empty();
    }
    return Optional.of(nodes.get(nodeId - 1));
  }

  public OptionalInt getNodeId(String nodeName) {
    return IntStream.range(0, nodes.size())
        .filter(idx -> nodeName.equals(nodes.get(idx).getNodeName()))
        .map(idx -> idx + 1)
        .findAny();
  }

  public OptionalInt getNodeId(InetSocketAddress nodeAddress) {
    return IntStream.range(0, nodes.size())
        .filter(idx -> nodes.get(idx).hasAddress(nodeAddress))
        .map(idx -> idx + 1)
        .findAny();
  }

  /**
   * Transform this model into a config file where all the "map" like settings can be expanded (one item per line)
   */
  @Override
  public Properties toProperties(boolean expanded, boolean includeDefaultValues) {
    Properties properties = new Properties();
    for (int i = 0; i < nodes.size(); i++) {
      String prefix = "node." + (i + 1) + ".";
      Properties props = nodes.get(i).toProperties(expanded, includeDefaultValues);
      props.stringPropertyNames().forEach(key -> properties.setProperty(prefix + key, props.getProperty(key)));
    }
    return properties;
  }

  @Override
  public Scope getScope() {
    return Scope.STRIPE;
  }
}
