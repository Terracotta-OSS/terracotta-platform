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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;


public class Cluster implements Cloneable {
  private final List<Stripe> stripes;

  @JsonCreator
  public Cluster(@JsonProperty("stripes") List<Stripe> stripes) {
    this.stripes = new CopyOnWriteArrayList<>(stripes);
  }

  public Cluster(Stripe... stripes) {
    this(Arrays.asList(stripes));
  }

  public List<Stripe> getStripes() {
    return Collections.unmodifiableList(stripes);
  }

  @JsonIgnore
  public boolean isEmpty() {
    return getNodeAddresses().isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Cluster cluster = (Cluster) o;
    return stripes.equals(cluster.stripes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stripes);
  }

  @Override
  public String toString() {
    return "Cluster{" +
        "stripes=" + stripes +
        '}';
  }

  public Optional<Stripe> getStripe(InetSocketAddress address) {
    return stripes.stream()
        .filter(stripe -> stripe.containsNode(address))
        .findFirst();
  }

  @JsonIgnore
  public Collection<InetSocketAddress> getNodeAddresses() {
    return stripes.stream().flatMap(stripe -> stripe.getNodes().stream()).map(Node::getNodeAddress).collect(Collectors.toSet());
  }

  public boolean containsNode(InetSocketAddress address) {
    return getStripe(address).isPresent();
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Cluster clone() {
    return new Cluster(stripes.stream().map(Stripe::clone).collect(toList()));
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
}
