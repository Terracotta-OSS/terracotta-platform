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
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.inet.HostPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Setting.CLIENT_LEASE_DURATION;
import static org.terracotta.dynamic_config.api.model.Setting.CLIENT_RECONNECT_WINDOW;
import static org.terracotta.dynamic_config.api.model.Setting.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.Setting.LOCK_CONTEXT;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUTHC;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_SSL_TLS;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_WHITELIST;

public class Cluster implements Cloneable, PropertyHolder {
  private List<Stripe> stripes;

  private UID uid;
  private String name;
  private LockContext lockContext;
  private Measure<TimeUnit> clientReconnectWindow;
  private Measure<TimeUnit> clientLeaseDuration;
  private String securityAuthc;
  private Boolean securitySslTls;
  private Boolean securityWhitelist;
  private FailoverPriority failoverPriority;
  private Map<String, Measure<MemoryUnit>> offheapResources;

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

  @Override
  public String getName() {
    return name;
  }

  @Override
  public UID getUID() {
    return uid;
  }

  public Cluster setUID(UID uid) {
    this.uid = requireNonNull(uid);
    return this;
  }

  public OptionalConfig<String> getSecurityAuthc() {
    return OptionalConfig.of(SECURITY_AUTHC, securityAuthc);
  }

  public OptionalConfig<Boolean> getSecuritySslTls() {
    return OptionalConfig.of(SECURITY_SSL_TLS, securitySslTls);
  }

  public OptionalConfig<Boolean> getSecurityWhitelist() {
    return OptionalConfig.of(SECURITY_WHITELIST, securityWhitelist);
  }

  public OptionalConfig<FailoverPriority> getFailoverPriority() {
    return OptionalConfig.of(FAILOVER_PRIORITY, failoverPriority);
  }

  public OptionalConfig<Measure<TimeUnit>> getClientReconnectWindow() {
    return OptionalConfig.of(CLIENT_RECONNECT_WINDOW, clientReconnectWindow);
  }

  public OptionalConfig<Measure<TimeUnit>> getClientLeaseDuration() {
    return OptionalConfig.of(CLIENT_LEASE_DURATION, clientLeaseDuration);
  }

  public OptionalConfig<Map<String, Measure<MemoryUnit>>> getOffheapResources() {
    return OptionalConfig.of(OFFHEAP_RESOURCES, offheapResources);
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

  public Cluster putOffheapResource(String name, long quantity, MemoryUnit memoryUnit) {
    return putOffheapResource(name, Measure.of(quantity, memoryUnit));
  }

  public Cluster putOffheapResource(String name, Measure<MemoryUnit> measure) {
    return putOffheapResources(singletonMap(name, measure));
  }

  public Cluster putOffheapResources(Map<String, Measure<MemoryUnit>> offheapResources) {
    if (this.offheapResources == null) {
      setOffheapResources(Optional.ofNullable(OFFHEAP_RESOURCES.<Map<String, Measure<MemoryUnit>>>getDefaultValue()).orElse(emptyMap()));
    }
    this.offheapResources.putAll(offheapResources);
    return this;
  }

  public Cluster setOffheapResources(Map<String, Measure<MemoryUnit>> offheapResources) {
    this.offheapResources = offheapResources == null ? null : new ConcurrentHashMap<>(offheapResources);
    return this;
  }

  public Cluster removeOffheapResource(String key) {
    if (this.offheapResources == null) {
      // this code is handling the removal of any default value set
      Map<String, Measure<MemoryUnit>> def = OFFHEAP_RESOURCES.getDefaultValue();
      if (def != null && def.containsKey(key)) {
        setOffheapResources(def);
      }
    }
    if (this.offheapResources != null) {
      this.offheapResources.remove(key);
    }
    return this;
  }

  public Cluster unsetOffheapResources() {
    Map<String, String> def = OFFHEAP_RESOURCES.getDefaultValue();
    setOffheapResources(def == null || def.isEmpty() ? null : emptyMap());
    return this;
  }

  public List<Stripe> getStripes() {
    return Collections.unmodifiableList(stripes);
  }

  public Cluster setStripes(List<Stripe> stripes) {
    this.stripes = new CopyOnWriteArrayList<>(stripes);
    return this;
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
    return stripes.isEmpty() || getNodes().isEmpty();
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
        Objects.equals(uid, that.uid) &&
        Objects.equals(lockContext, that.lockContext) &&
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
    return Objects.hash(
        stripes, name, securityAuthc, securitySslTls, securityWhitelist, uid,
        failoverPriority, clientReconnectWindow, clientLeaseDuration, offheapResources, lockContext
    );
  }

  @Override
  public String toString() {
    return Props.toString(toProperties(false, false, true));
  }

  public String toShapeString() {
    return (name == null ? "<no name>" : name) + " ( " + stripes.stream().map(Stripe::toShapeString).collect(joining(", ")) + " )";
  }

  public Collection<Endpoint> getInternalEndpoints() {
    return getNodes().stream().map(Node::getInternalEndpoint).collect(toList());
  }

  public boolean containsNode(UID nodeUID) {
    return getNode(nodeUID).isPresent();
  }

  public boolean containsNode(String nodeName) {
    return getNodes().stream().map(Node::getName).anyMatch(isEqual(nodeName));
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Cluster clone() {
    final Cluster clone = new Cluster(stripes.stream().map(Stripe::clone).collect(toList()));
    clone.clientLeaseDuration = this.clientLeaseDuration;
    clone.clientReconnectWindow = this.clientReconnectWindow;
    clone.failoverPriority = this.failoverPriority;
    clone.lockContext = this.lockContext;
    clone.name = this.name;
    clone.uid = this.uid;
    clone.offheapResources = this.offheapResources == null ? null : new ConcurrentHashMap<>(this.offheapResources);
    clone.securityAuthc = this.securityAuthc;
    clone.securitySslTls = this.securitySslTls;
    clone.securityWhitelist = this.securityWhitelist;
    return clone;
  }

  public boolean removeStripe(Stripe stripe) {
    return stripes.remove(stripe);
  }

  public boolean removeStripe(UID stripeUID) {
    return stripes.removeIf(stripe -> stripe.getUID().equals(stripeUID));
  }

  public boolean removeNode(UID uid) {
    boolean detached = stripes.stream().anyMatch(stripe -> stripe.removeNode(uid));
    if (detached) {
      stripes.removeIf(Stripe::isEmpty);
    }
    return detached;
  }

  public Optional<Node> getNode(UID nodeUID) {
    return stripes.stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .filter(node -> node.getUID().equals(nodeUID))
        .findAny();
  }

  public Optional<Node> getNodeByName(String name) {
    return stripes.stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .filter(node -> node.getName().equals(name))
        .findAny();
  }

  public Optional<Stripe> getStripe(UID stripeUID) {
    return getStripes().stream().filter(s -> s.getUID().equals(stripeUID)).findAny();
  }

  public Optional<Stripe> getStripeByName(String name) {
    return getStripes().stream().filter(s -> s.getName().equals(name)).findAny();
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

  public OptionalInt getNodeId(UID nodeUID) {
    List<Node> nodes = stripes.stream()
        .filter(s -> s.containsNode(nodeUID))
        .findAny().get().getNodes();
    return IntStream.range(0, nodes.size())
        .filter(idx -> nodes.get(idx).getUID().equals(nodeUID))
        .map(idx -> idx + 1)
        .findAny();
  }

  public OptionalInt getStripeId(UID stripeUID) {
    return IntStream.range(0, stripes.size())
        .filter(idx -> stripes.get(idx).getUID().equals(stripeUID))
        .map(idx -> idx + 1)
        .findAny();
  }

  public OptionalInt getStripeIdByNode(UID nodeUID) {
    return IntStream.range(0, stripes.size())
        .filter(idx -> stripes.get(idx).containsNode(nodeUID))
        .map(idx -> idx + 1)
        .findAny();
  }

  public Optional<Stripe> getStripeByNode(UID nodeUID) {
    return getStripes().stream().filter(s -> s.containsNode(nodeUID)).findAny();
  }

  public Optional<Stripe> getStripeByNodeName(String nodeName) {
    return getStripes().stream().filter(s -> s.containsNode(nodeName)).findAny();
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
  public Properties toProperties(boolean expanded, boolean includeDefaultValues, boolean includeHiddenSettings, Version version) {
    Properties properties = Setting.modelToProperties(this, expanded, includeDefaultValues, includeHiddenSettings, version);
    for (int i = 0; i < stripes.size(); i++) {
      String prefix = "stripe." + (i + 1) + ".";
      Properties props = stripes.get(i).toProperties(expanded, includeDefaultValues, includeHiddenSettings, version);
      props.stringPropertyNames().forEach(key -> properties.setProperty(prefix + key, props.getProperty(key)));
    }
    return properties;
  }

  @Override
  public Stream<? extends PropertyHolder> descendants() {
    return concat(stripes.stream(), stripes.stream().flatMap(Stripe::descendants));
  }

  public Collection<String> getDataDirNames() {
    return getNodes().stream().flatMap(node -> node.getDataDirs().orDefault().keySet().stream()).collect(toSet());
  }

  public Cluster removeStripes() {
    stripes.clear();
    return this;
  }

  public OptionalConfig<LockContext> getConfigurationLockContext() {
    return OptionalConfig.of(LOCK_CONTEXT, lockContext);
  }

  public Cluster setConfigurationLockContext(LockContext lockContext) {
    this.lockContext = lockContext;
    return this;
  }

  public Optional<Stripe> inSameStripe(UID... nodeUIDs) {
    Set<UID> uids = new HashSet<>();
    for (UID nodeUID : nodeUIDs) {
      Optional<Stripe> stripe = getStripeByNode(nodeUID);
      if (!stripe.isPresent()) {
        return Optional.empty();
      } else {
        uids.add(stripe.get().getUID());
      }
    }
    return uids.size() == 1 ? Optional.of(uids.iterator().next()).flatMap(this::getStripe) : Optional.empty();
  }

  /**
   * Generate a new UID that is not yet used within this cluster
   */
  public UID newUID() {
    Set<UID> uuids = concat(of(this), descendants()).map(PropertyHolder::getUID).filter(Objects::nonNull).collect(toSet());
    UID uuid;
    while (uuids.contains(uuid = UID.newUID())) ;
    return uuid;
  }

  /**
   * Generate a new UID that is not yet used within this cluster and which randomness is controlled
   */
  public UID newUID(Random random) {
    Set<UID> uuids = concat(of(this), descendants()).map(PropertyHolder::getUID).filter(Objects::nonNull).collect(toSet());
    UID uuid;
    while (uuids.contains(uuid = UID.newUID(random))) ;
    return uuid;
  }

  public Optional<Node> findReachableNode(HostPort hostPort) {
    return stripes.stream()
        .map(stripe -> stripe.findReachableNode(hostPort).orElse(null))
        .filter(Objects::nonNull)
        .findFirst();
  }

  /**
   * Search all endpoints of all nodes matching this address. A result can contain several endpoints of the same node
   */
  public Collection<? extends Endpoint> search(Collection<? extends HostPort> hostPorts) {
    return hostPorts.stream()
        .flatMap(addr -> getNodes().stream().flatMap(node -> node.findEndpoints(addr).stream()))
        .collect(toList());
  }

  public EndpointType determineEndpointType(HostPort... initiators) {
    return determineEndpointType(Arrays.asList(initiators));
  }

  public EndpointType determineEndpointType(Collection<? extends HostPort> initiators) {
    // Search in the topology the endpoints matching exactly the initiators.
    //
    // Note: A node can be returned several times for 2 different endpoint types if its configuration has the same address and port
    // for example in both internal and public addresses.
    //
    // If no result, use "determineEndpoints()".
    // If more than 1 result, group the endpoint type by counts and return the one having most of the matches
    //
    // Note: in case the same address and port would be set to all nodes on both internal and public addresses,
    // then we return a random endpoint type since there is no way to know what the user wanted to do.
    return search(initiators)
        .stream()
        .collect(collectingAndThen(groupingBy(Endpoint::getEndpointType, counting()), map -> map.entrySet().stream()))
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElseGet(() -> determineEndpoints().iterator().next().getEndpointType());
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
   * @param initiators Addresses used to load this class, can be null.
   */
  public Collection<Endpoint> determineEndpoints(HostPort... initiators) {
    return determineEndpoints(Arrays.asList(initiators));
  }

  public Collection<Endpoint> determineEndpoints(Collection<? extends HostPort> initiators) {
    return determineEndpoints(determineEndpointType(initiators));
  }

  public Collection<Endpoint> determineEndpoints() {
    return getNodes().stream().map(Node::determineEndpoint).collect(toList());
  }

  public Optional<Endpoint> determineEndpoint(UID nodeUID, HostPort... initiators) {
    return determineEndpoint(nodeUID, Arrays.asList(initiators));
  }

  public Optional<Endpoint> determineEndpoint(UID nodeUID, Collection<? extends HostPort> initiators) {
    final EndpointType endpointType = determineEndpointType(initiators);
    return determineEndpoint(nodeUID, endpointType);
  }

  public Optional<Endpoint> determineEndpoint(UID nodeUID, EndpointType endpointType) {
    return getNode(nodeUID).map(node -> node.determineEndpoint(endpointType));
  }

  public Collection<Endpoint> determineEndpoints(Endpoint initiator) {
    return determineEndpoints(initiator.getEndpointType());
  }

  public Collection<Endpoint> determineEndpoints(EndpointType endpointType) {
    return getNodes().stream().map(node -> node.determineEndpoint(endpointType)).collect(toList());
  }
}
