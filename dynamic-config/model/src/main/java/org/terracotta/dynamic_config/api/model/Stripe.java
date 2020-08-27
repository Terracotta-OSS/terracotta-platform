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
import org.terracotta.dynamic_config.api.service.Props;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;


public class Stripe implements Cloneable, PropertyHolder {

  private List<Node> nodes = new CopyOnWriteArrayList<>();

  private UID uid;
  private String name;

  public List<Node> getNodes() {
    return Collections.unmodifiableList(nodes);
  }

  public Stripe setNodes(List<Node> nodes) {
    this.nodes = new CopyOnWriteArrayList<>(nodes);
    return this;
  }

  @Override
  public String getName() {
    return name;
  }

  public Stripe setName(String name) {
    this.name = requireNonNull(name);
    return this;
  }

  @Override
  public UID getUID() {
    return uid;
  }

  public Stripe setUID(UID uid) {
    this.uid = requireNonNull(uid);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Stripe stripe = (Stripe) o;
    return nodes.equals(stripe.nodes)
        && Objects.equals(name, stripe.name)
        && Objects.equals(uid, stripe.uid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodes, name, uid);
  }

  @Override
  public String toString() {
    return Props.toString(toProperties(false, false, true));
  }

  public String toShapeString() {
    return name + ":" + uid + " ( " + nodes.stream().map(Node::toShapeString).collect(joining(", ")) + " )";
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

  public Optional<Node> getNode(UID uid) {
    return nodes.stream().filter(node -> node.getUID().equals(uid)).findAny();
  }

  public Optional<Node> getNode(String nodeName) {
    return nodes.stream().filter(node -> node.getName().equals(nodeName)).findAny();
  }

  public boolean containsNode(UID uid) {
    return nodes.stream().anyMatch(node -> node.getUID().equals(uid));
  }

  public boolean containsNode(String nodeName) {
    return nodes.stream().anyMatch(node -> node.getName().equals(nodeName));
  }

  /**
   * Returns the endpoints to use to connect to the nodes of the cluster.
   * The endpoints are using the same address "group" as the address used to access
   * this node context.
   * <p>
   * In case no initiator is given, or if it is not found, the returned endpoints
   * will be the public addresses if all nodes have a public address, otherwise it
   * will be the internal addresses
   *
   * @param initiator Address used to load this class, can be null.
   */
  public Collection<Node.Endpoint> getEndpoints(InetSocketAddress initiator) {
    Function<Node, Node.Endpoint> fetcher = getEndpointFetcher(initiator);
    return getNodes().stream().map(fetcher).collect(toList());
  }

  public Collection<Node.Endpoint> getSimilarEndpoints(Node.Endpoint initiator) {
    return getNodes().stream().map(node -> node.getSimilarEndpoint(initiator)).collect(toList());
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Stripe clone() {
    Stripe copy = new Stripe();
    copy.nodes = this.nodes.stream().map(Node::clone).collect(toCollection(CopyOnWriteArrayList::new));
    copy.name = this.name;
    copy.uid = this.uid;
    return copy;
  }

  public boolean removeNode(UID uid) {
    return nodes.removeIf(node -> node.getUID().equals(uid));
  }

  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  public Stripe addNode(Node source) {
    nodes.add(source);
    return this;
  }

  public Stripe addNodes(Node... sources) {
    for (Node source : sources) {
      addNode(source);
    }
    return this;
  }

  public int getNodeCount() {
    return nodes.size();
  }

  /**
   * Transform this model into a config file where all the "map" like settings can be expanded (one item per line)
   */
  @Override
  public Properties toProperties(boolean expanded, boolean includeDefaultValues, boolean includeHiddenSettings, Version version) {
    Properties properties = Setting.modelToProperties(this, expanded, includeDefaultValues, includeHiddenSettings, version);
    for (int i = 0; i < nodes.size(); i++) {
      String prefix = "node." + (i + 1) + ".";
      Properties props = nodes.get(i).toProperties(expanded, includeDefaultValues, includeHiddenSettings, version);
      props.stringPropertyNames().forEach(key -> properties.setProperty(prefix + key, props.getProperty(key)));
    }
    return properties;
  }

  @Override
  public Stream<? extends PropertyHolder> descendants() {
    return nodes.stream();
  }

  @Override
  public Scope getScope() {
    return Scope.STRIPE;
  }

  /**
   * Finds an address group based on an address given by the user
   * to connect to a node
   */
  private Function<Node, Node.Endpoint> getEndpointFetcher(InetSocketAddress initiator) {
    boolean publicAddressConfigured = true;
    for (Node node : getNodes()) {
      if (node.getInternalAddress().equals(initiator)) {
        return Node::getInternalEndpoint;
      }
      Optional<InetSocketAddress> publicAddress = node.getPublicAddress();
      publicAddressConfigured &= publicAddress.isPresent();
      if (publicAddress.isPresent() && publicAddress.get().equals(initiator)) {
        return n -> n.getPublicEndpoint().get();
      }
    }
    // we didn't find any exact match...
    // if the nodes have public addresses, then use them
    return publicAddressConfigured ?
        n -> n.getPublicEndpoint().get() :
        Node::getInternalEndpoint;
  }
}
