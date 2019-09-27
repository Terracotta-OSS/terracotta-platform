/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.model.Setting.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.model.Setting.LICENSE_FILE;
import static com.terracottatech.dynamic_config.model.Setting.NODE_REPOSITORY_DIR;
import static com.terracottatech.utilities.Tuple2.tuple2;
import static java.util.Comparator.comparing;
import static java.util.EnumSet.complementOf;
import static java.util.EnumSet.of;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;


public class Cluster implements Cloneable {
  private final List<Stripe> stripes;

  private String name;

  @JsonCreator
  public Cluster(@JsonProperty("name") String name,
                 @JsonProperty(value = "stripes", required = true) List<Stripe> stripes) {
    this.stripes = new CopyOnWriteArrayList<>(requireNonNull(stripes));
    this.name = name;
  }

  public Cluster(List<Stripe> stripes) {
    this(null, stripes);
  }

  public Cluster(String name, Stripe... stripes) {
    this(name, Arrays.asList(stripes));
  }

  public Cluster(Stripe... stripes) {
    this(null, Arrays.asList(stripes));
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
    return getNodeAddresses().isEmpty();
  }

  /**
   * @return The only node (if available) in the only stripe of this cluster.
   * @throws IllegalStateException if the cluster has more than 1 stripe or more than 1 node
   */
  @JsonIgnore
  public Optional<Node> getSingleNode() throws IllegalStateException {
    if (stripes.size() > 1) {
      throw new IllegalStateException();
    }
    Stripe s = stripes.iterator().next();
    return s.getSingleNode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Cluster)) return false;
    Cluster cluster = (Cluster) o;
    return Objects.equals(getStripes(), cluster.getStripes()) && Objects.equals(getName(), cluster.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getStripes(), getName());
  }

  @Override
  public String toString() {
    return "Cluster{" + "name='" + name + "', stripes='" + stripes + '}';
  }

  public Optional<Stripe> getStripe(InetSocketAddress address) {
    return stripes.stream()
        .filter(stripe -> stripe.containsNode(address))
        .findFirst();
  }

  @JsonIgnore
  public Collection<InetSocketAddress> getNodeAddresses() {
    return stripes.stream().flatMap(stripe -> stripe.getNodes().stream()).map(Node::getNodeAddress).collect(toList());
  }

  public boolean containsNode(InetSocketAddress address) {
    return getStripe(address).isPresent();
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Cluster clone() {
    return new Cluster(name, stripes.stream().map(Stripe::clone).collect(toList()));
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
        .filter(node -> node.getNodeAddress().equals(nodeAddress))
        .findFirst();
  }

  public Optional<Integer> getStripeId(InetSocketAddress address) {
    return getStripe(address)
        .map(stripes::indexOf)
        .filter(idx -> idx >= 0)
        .map(idx -> idx + 1);
  }

  public Optional<Integer> getStripeId(Node me) {
    return getStripeId(me.getNodeAddress());
  }

  @JsonIgnore
  public int getNodeCount() {
    return stripes.stream().mapToInt(Stripe::getNodeCount).sum();
  }

  @JsonIgnore
  public Collection<Node> getNodes() {
    return stripes.stream().flatMap(s -> s.getNodes().stream()).collect(toList());
  }

  public Optional<Node> getNode(int stripeId, String nodeName) {
    return stripes.get(stripeId - 1).getNode(nodeName);
  }

  public Node getNode(int stripeId, int nodeId) {
    return stripes.get(stripeId - 1).getNodes().get(nodeId - 1);
  }

  public void forEach(BiConsumer<Integer, Node> consumer) {
    List<Stripe> stripes = getStripes();
    for (int i = 0; i < stripes.size(); i++) {
      int stripeId = i + 1;
      stripes.get(0).getNodes().forEach(node -> consumer.accept(stripeId, node));
    }
  }

  /**
   * Transform this model into a config file
   */
  public Properties toProperties() {
    return toProperties(false);
  }

  /**
   * Transform this model into a config file where all the "map" like settings can be expanded (one item per line)
   */
  public Properties toProperties(boolean expanded) {
    // select the settings to output and sort them.
    List<Setting> settings = complementOf(of(CLUSTER_NAME, LICENSE_FILE, NODE_REPOSITORY_DIR))
        .stream()
        .sorted(comparing(Setting::toString))
        .collect(toList());
    // iterate over all stripes
    return rangeClosed(1, stripes.size()).boxed().flatMap(stripeId -> {
      List<Node> nodes = stripes.get(stripeId - 1).getNodes();
      // iterate over all nodes of this stripe
      return rangeClosed(1, nodes.size()).boxed().flatMap(nodeId -> {
        Node node = nodes.get(nodeId - 1);
        // for each setting, create the line:
        // stripe.<ids>.node.<idx>.<setting>=<value> or stripe.<ids>.node.<idx>.<setting>.<key>=<value>
        // depending whether we want teh expanded or non expanded form
        return settings.stream()
            .flatMap(setting -> expanded && setting.isMap() ?
                setting.getExpandedProperties(node).map(property -> tuple2("stripe." + stripeId + ".node." + nodeId + "." + setting + "." + property.t1, property.t2)) :
                Stream.of(tuple2("stripe." + stripeId + ".node." + nodeId + "." + setting, setting.getPropertyValue(node).orElse(""))));
      });
    }).reduce(new Properties(), (props, tupe) -> {
      // then reducing all these lines into a property object
      props.setProperty(tupe.t1, tupe.t2);
      return props;
    }, (p1, p2) -> {
      throw new UnsupportedOperationException();
    });
  }
}
