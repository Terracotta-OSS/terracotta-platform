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
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;

public class Cluster implements Cloneable, PropertyHolder {
  private final List<Stripe> stripes;

  private String name;
  private Measure<TimeUnit> clientReconnectWindow;
  private Measure<TimeUnit> clientLeaseDuration;
  private String securityAuthc;
  private Boolean securitySslTls;
  private Boolean securityWhitelist;
  private FailoverPriority failoverPriority;
  private final Map<String, Measure<MemoryUnit>> offheapResources = new ConcurrentHashMap<>();

  public Cluster(List<Stripe> stripes) {
    this.stripes = new CopyOnWriteArrayList<>(requireNonNull(stripes));
  }

  public Cluster(Stripe... stripes) {
    this(Arrays.asList(stripes));
  }

  @Override
  public Scope getScope() {
    return CLUSTER;
  }

  public String getName() {
    return name;
  }

  public String getSecurityAuthc() {
    return securityAuthc;
  }

  public Boolean isSecuritySslTls() {
    return securitySslTls;
  }

  public Boolean isSecurityWhitelist() {
    return securityWhitelist;
  }

  public FailoverPriority getFailoverPriority() {
    return failoverPriority;
  }

  public Measure<TimeUnit> getClientReconnectWindow() {
    return clientReconnectWindow;
  }

  public Measure<TimeUnit> getClientLeaseDuration() {
    return clientLeaseDuration;
  }

  public Map<String, Measure<MemoryUnit>> getOffheapResources() {
    return Collections.unmodifiableMap(offheapResources);
  }

  public Cluster setSecurityAuthc(String securityAuthc) {
    this.securityAuthc = securityAuthc;
    return this;
  }

  public Cluster setSecuritySslTls(Boolean securitySslTls) {
    this.securitySslTls = securitySslTls;
    return this;
  }

  public Cluster setSecurityWhitelist(Boolean securityWhitelist) {
    this.securityWhitelist = securityWhitelist;
    return this;
  }

  public Cluster setFailoverPriority(FailoverPriority failoverPriority) {
    this.failoverPriority = failoverPriority;
    return this;
  }

  public Cluster setClientReconnectWindow(long clientReconnectWindow, TimeUnit timeUnit) {
    return setClientReconnectWindow(Measure.of(clientReconnectWindow, timeUnit));
  }

  public Cluster setClientReconnectWindow(long clientReconnectWindow, java.util.concurrent.TimeUnit jdkUnit) {
    return setClientReconnectWindow(Measure.of(clientReconnectWindow, TimeUnit.from(jdkUnit).orElseThrow(() -> new IllegalArgumentException(jdkUnit.name()))));
  }

  public Cluster setClientReconnectWindow(Measure<TimeUnit> measure) {
    this.clientReconnectWindow = measure;
    return this;
  }

  public Cluster setClientLeaseDuration(long clientLeaseDuration, TimeUnit timeUnit) {
    return setClientLeaseDuration(Measure.of(clientLeaseDuration, timeUnit));
  }

  public Cluster setClientLeaseDuration(long clientLeaseDuration, java.util.concurrent.TimeUnit jdkUnit) {
    return setClientLeaseDuration(Measure.of(clientLeaseDuration, TimeUnit.from(jdkUnit).orElseThrow(() -> new IllegalArgumentException(jdkUnit.name()))));
  }

  public Cluster setClientLeaseDuration(Measure<TimeUnit> measure) {
    this.clientLeaseDuration = measure;
    return this;
  }

  public Cluster setOffheapResource(String name, long quantity, MemoryUnit memoryUnit) {
    return setOffheapResource(name, Measure.of(quantity, memoryUnit));
  }

  public Cluster setOffheapResource(String name, Measure<MemoryUnit> measure) {
    this.offheapResources.put(name, measure);
    return this;
  }

  public Cluster setOffheapResources(Map<String, Measure<MemoryUnit>> offheapResources) {
    this.offheapResources.putAll(offheapResources);
    return this;
  }

  public Cluster removeOffheapResource(String key) {
    this.offheapResources.remove(key);
    return this;
  }

  public Cluster clearOffheapResources() {
    this.offheapResources.clear();
    return this;
  }

  public List<Stripe> getStripes() {
    return Collections.unmodifiableList(stripes);
  }

  public Cluster addStripe(Stripe stripe) {
    stripes.add(stripe);
    return this;
  }

  public Cluster setName(String name) {
    this.name = name;
    return this;
  }

  public boolean isEmpty() {
    return stripes.isEmpty() || getNodeAddresses().isEmpty();
  }

  /**
   * @return The only node (if available) in the only stripe of this cluster.
   * @throws IllegalStateException if the cluster has more than 1 stripe or more than 1 node
   */
  public Optional<Node> getSingleNode() throws IllegalStateException {
    return getSingleStripe().flatMap(Stripe::getSingleNode);
  }

  public Optional<Stripe> getSingleStripe() {
    if (stripes.size() > 1) {
      throw new IllegalStateException();
    }
    if (stripes.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(stripes.iterator().next());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Cluster)) return false;
    Cluster that = (Cluster) o;
    return Objects.equals(stripes, that.stripes) &&
        Objects.equals(name, that.name) &&
        Objects.equals(securitySslTls, that.securitySslTls) &&
        Objects.equals(securityWhitelist, that.securityWhitelist) &&
        Objects.equals(securityAuthc, that.securityAuthc) &&
        Objects.equals(failoverPriority, that.failoverPriority) &&
        Objects.equals(clientReconnectWindow, that.clientReconnectWindow) &&
        Objects.equals(clientLeaseDuration, that.clientLeaseDuration) &&
        Objects.equals(offheapResources, that.offheapResources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stripes, name, securityAuthc, securitySslTls, securityWhitelist,
        failoverPriority, clientReconnectWindow, clientLeaseDuration, offheapResources);
  }

  @Override
  public String toString() {
    return "Cluster{" +
        "name='" + name + '\'' +
        ", securityAuthc='" + securityAuthc + '\'' +
        ", securitySslTls=" + securitySslTls +
        ", securityWhitelist=" + securityWhitelist +
        ", failoverPriority='" + failoverPriority + '\'' +
        ", clientReconnectWindow=" + clientReconnectWindow +
        ", clientLeaseDuration=" + clientLeaseDuration +
        ", offheapResources=" + offheapResources +
        ", stripes='" + stripes +
        '}';
  }

  public String toShapeString() {
    return "Cluster '" + name + "' ( " + stripes.stream().map(Stripe::toShapeString).collect(joining(", ")) + " )";
  }

  public Optional<Stripe> getStripe(InetSocketAddress address) {
    return stripes.stream()
        .filter(stripe -> stripe.containsNode(address))
        .findAny();
  }

  public Collection<InetSocketAddress> getNodeAddresses() {
    return stripes.stream().flatMap(stripe -> stripe.getNodes().stream()).map(Node::getAddress).collect(toList());
  }

  public boolean containsNode(InetSocketAddress address) {
    return getStripe(address).isPresent();
  }

  public boolean containsNode(int stripeId, String nodeName) {
    return getStripe(stripeId).flatMap(stripe -> stripe.getNode(nodeName)).isPresent();
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Cluster clone() {
    return new Cluster(stripes.stream().map(Stripe::clone).collect(toList()))
        .setName(name)
        .setClientLeaseDuration(clientLeaseDuration)
        .setClientReconnectWindow(clientReconnectWindow)
        .setFailoverPriority(failoverPriority)
        .setOffheapResources(offheapResources)
        .setSecurityAuthc(securityAuthc)
        .setSecuritySslTls(securitySslTls)
        .setSecurityWhitelist(securityWhitelist);
  }

  public boolean removeStripe(Stripe stripe) {
    return stripes.remove(stripe);
  }

  public boolean removeNode(InetSocketAddress address) {
    boolean detached = stripes.stream().anyMatch(stripe -> stripe.removeNode(address));
    if (detached) {
      stripes.removeIf(Stripe::isEmpty);
    }
    return detached;
  }

  public Optional<Node> getNode(InetSocketAddress nodeAddress) {
    return stripes.stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .filter(node -> node.hasAddress(nodeAddress))
        .findAny();
  }

  public OptionalInt getStripeId(InetSocketAddress address) {
    return IntStream.range(0, stripes.size())
        .filter(idx -> stripes.get(idx).containsNode(address))
        .map(idx -> idx + 1)
        .findAny();
  }

  public OptionalInt getNodeId(InetSocketAddress address) {
    return stripes.stream()
        .map(stripe -> stripe.getNodeId(address))
        .filter(OptionalInt::isPresent)
        .findAny()
        .orElse(OptionalInt.empty());
  }

  public OptionalInt getNodeId(int stripeId, String nodeName) {
    return getStripe(stripeId)
        .map(stripe -> IntStream.range(0, stripe.getNodeCount())
            .filter(idx -> nodeName.equals(stripe.getNodes().get(idx).getName()))
            .map(idx -> idx + 1)
            .findAny())
        .orElse(OptionalInt.empty());
  }

  public Optional<Stripe> getStripe(int stripeId) {
    if (stripeId < 1) {
      throw new IllegalArgumentException("Invalid stripe ID: " + stripeId);
    }
    if (stripeId > stripes.size()) {
      return Optional.empty();
    }
    return Optional.of(stripes.get(stripeId - 1));
  }

  public int getNodeCount() {
    return stripes.stream().mapToInt(Stripe::getNodeCount).sum();
  }

  public int getStripeCount() {
    return stripes.size();
  }

  public Collection<Node> getNodes() {
    return stripes.stream().flatMap(s -> s.getNodes().stream()).collect(toList());
  }

  public Optional<Node> getNode(int stripeId, String nodeName) {
    return getStripe(stripeId).flatMap(stripe -> stripe.getNode(nodeName));
  }

  public Optional<Node> getNode(int stripeId, int nodeId) {
    return getStripe(stripeId).flatMap(stripe -> stripe.getNode(nodeId));
  }

  public Stream<NodeContext> nodeContexts() {
    return rangeClosed(1, stripes.size())
        .boxed()
        .flatMap(stripeId -> stripes.get(stripeId - 1).getNodes()
            .stream()
            .map(Node::getName)
            .map(name -> new NodeContext(this, stripeId, name)));
  }

  public void forEach(BiConsumer<Integer, Node> consumer) {
    List<Stripe> stripes = getStripes();
    for (int i = 0; i < stripes.size(); i++) {
      int stripeId = i + 1;
      stripes.get(stripeId - 1).getNodes().forEach(node -> consumer.accept(stripeId, node));
    }
  }

  /**
   * Transform this model into a config file where all the "map" like settings can be expanded (one item per line)
   */
  @Override
  public Properties toProperties(boolean expanded, boolean includeDefaultValues) {
    Properties properties = Setting.modelToProperties(this, expanded, includeDefaultValues);
    for (int i = 0; i < stripes.size(); i++) {
      String prefix = "stripe." + (i + 1) + ".";
      Properties props = stripes.get(i).toProperties(expanded, includeDefaultValues);
      props.stringPropertyNames().forEach(key -> properties.setProperty(prefix + key, props.getProperty(key)));
    }
    return properties;
  }

  public Collection<String> getDataDirNames() {
    return getNodes().stream().flatMap(node -> node.getDataDirs().keySet().stream()).collect(toSet());
  }

  public Cluster fillRequiredSettings() {
    return Setting.fillRequiredSettings(this);
  }

  public Cluster removeStripes() {
    stripes.clear();
    return this;
  }
}
