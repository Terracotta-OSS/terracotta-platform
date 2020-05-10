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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

import static java.util.Collections.emptyList;
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
  private boolean securitySslTls = Boolean.parseBoolean(Setting.SECURITY_SSL_TLS.getDefaultValue());
  private boolean securityWhitelist = Boolean.parseBoolean(Setting.SECURITY_WHITELIST.getDefaultValue());
  private FailoverPriority failoverPriority = FailoverPriority.availability();
  private final Map<String, Measure<MemoryUnit>> offheapResources = new ConcurrentHashMap<>();

  @JsonCreator
  Cluster(@JsonProperty("name") String name,
          @JsonProperty(value = "stripes", required = true) List<Stripe> stripes) {
    this.stripes = new CopyOnWriteArrayList<>(requireNonNull(stripes));
    this.name = name;
  }

  @Override
  @JsonIgnore
  public Scope getScope() {
    return CLUSTER;
  }

  public String getSecurityAuthc() {
    return securityAuthc;
  }

  public boolean isSecuritySslTls() {
    return securitySslTls;
  }

  public boolean isSecurityWhitelist() {
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

  public Cluster setSecuritySslTls(boolean securitySslTls) {
    this.securitySslTls = securitySslTls;
    return this;
  }

  public Cluster setSecurityWhitelist(boolean securityWhitelist) {
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

  public String getName() {
    return name;
  }

  public Cluster setName(String name) {
    this.name = name;
    return this;
  }

  @JsonIgnore
  public boolean isEmpty() {
    return stripes.isEmpty() || getNodeAddresses().isEmpty();
  }

  /**
   * @return The only node (if available) in the only stripe of this cluster.
   * @throws IllegalStateException if the cluster has more than 1 stripe or more than 1 node
   */
  @JsonIgnore
  public Optional<Node> getSingleNode() throws IllegalStateException {
    return getSingleStripe().flatMap(Stripe::getSingleNode);
  }

  @JsonIgnore
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
        securitySslTls == that.securitySslTls &&
        securityWhitelist == that.securityWhitelist &&
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

  @JsonIgnore
  public Collection<InetSocketAddress> getNodeAddresses() {
    return stripes.stream().flatMap(stripe -> stripe.getNodes().stream()).map(Node::getNodeAddress).collect(toList());
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
    return new Cluster(name, stripes.stream().map(Stripe::clone).collect(toList()))
        .setClientLeaseDuration(clientLeaseDuration)
        .setClientReconnectWindow(clientReconnectWindow)
        .setFailoverPriority(failoverPriority)
        .setOffheapResources(offheapResources)
        .setSecurityAuthc(securityAuthc)
        .setSecuritySslTls(securitySslTls)
        .setSecurityWhitelist(securityWhitelist);
  }

  public Cluster attachStripe(Stripe stripe) {
    if (isEmpty()) {
      throw new IllegalStateException("Empty cluster.");
    }

    List<String> duplicates = stripe.getNodes().stream()
        .map(Node::getNodeAddress)
        .filter(this::containsNode)
        .map(InetSocketAddress::toString)
        .collect(toList());
    if (!duplicates.isEmpty()) {
      throw new IllegalArgumentException("Nodes are already in the cluster: " + String.join(", ", duplicates) + ".");
    }

    Node aNode = stripes.iterator().next().getNodes().iterator().next();
    Stripe newStripe = stripe.cloneForAttachment(aNode);
    stripes.add(newStripe);

    return this;
  }

  public boolean detachStripe(Stripe stripe) {
    return stripes.remove(stripe);
  }

  public boolean detachNode(InetSocketAddress address) {
    boolean detached = stripes.stream().anyMatch(stripe -> stripe.detachNode(address));
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
            .filter(idx -> nodeName.equals(stripe.getNodes().get(idx).getNodeName()))
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

  @JsonIgnore
  public int getNodeCount() {
    return stripes.stream().mapToInt(Stripe::getNodeCount).sum();
  }

  @JsonIgnore
  public int getStripeCount() {
    return stripes.size();
  }

  @JsonIgnore
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
            .map(Node::getNodeName)
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

  @JsonIgnore
  public Collection<String> getDataDirNames() {
    return getNodes().stream().flatMap(node -> node.getDataDirs().keySet().stream()).collect(toSet());
  }

  public Cluster fillRequiredSettings() {
    return Setting.fillRequiredSettings(this);
  }

  private Cluster fillSettings() {
    return Setting.fillSettings(this);
  }

  public Cluster removeStripes() {
    stripes.clear();
    return this;
  }

  public static Cluster newDefaultCluster() {
    return newDefaultCluster((String) null);
  }

  public static Cluster newDefaultCluster(String name) {
    return new Cluster(name, emptyList())
        .fillSettings();
  }

  public static Cluster newDefaultCluster(Stripe... stripes) {
    return newCluster(stripes)
        .fillSettings();
  }

  public static Cluster newDefaultCluster(String name, Stripe... stripes) {
    return new Cluster(name, Arrays.asList(stripes))
        .fillSettings();
  }

  public static Cluster newCluster(Stripe... stripes) {
    return new Cluster(null, Arrays.asList(stripes));
  }
}
