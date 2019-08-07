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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;


public class Stripe implements Cloneable {
  private final Collection<Node> nodes;

  @JsonCreator
  public Stripe(@JsonProperty("nodes") Collection<Node> nodes) {
    this.nodes = new CopyOnWriteArrayList<>(nodes);
  }

  public Stripe(Node... nodes) {
    this(Arrays.asList(nodes));
  }

  public Stripe() {
    this.nodes = new ArrayList<>();
  }

  public Collection<Node> getNodes() {
    return Collections.unmodifiableCollection(nodes);
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

  @JsonIgnore
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
    return nodes.stream().filter(node -> node.getNodeAddress().equals(address)).findFirst();
  }

  public boolean containsNode(InetSocketAddress address) {
    return nodes.stream().map(Node::getNodeAddress).anyMatch(isEqual(address));
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Stripe clone() {
    return new Stripe(nodes.stream().map(Node::clone).collect(toList()));
  }

  // please keep this package-local:
  // detachment of a node should be handled by cluster object
  boolean detachNode(InetSocketAddress address) {
    return nodes.removeIf(node -> Objects.equals(node.getNodeAddress(), address));
  }

  @JsonIgnore
  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  /**
   * Attach a node to this stripe.
   * <p>
   * The node parameters are expected to be validated before.
   * <p>
   * Also, we cannot attach a node to an empty stripe: this is impossible since
   * attachment needs a source and destination node, which belongs to a non-empty stripe
   */
  public Stripe attachNode(Node source) {
    if (containsNode(source.getNodeAddress())) {
      throw new IllegalArgumentException("Node " + source.getNodeAddress() + " is already in the stripe.");
    }
    if (isEmpty()) {
      throw new IllegalStateException("Empty stripe.");
    }
    Node aNode = nodes.iterator().next();
    Node newNode = source.cloneForAttachment(aNode);
    addNode(newNode);
    return this;
  }

  public Stripe addNode(Node source) {
    nodes.add(source);
    return this;
  }

  public Stripe addNodes(Collection<Node> sources) {
    nodes.addAll(sources);
    return this;
  }

  public Stripe cloneForAttachment(Node aNodeFromTargetCluster) {
    return nodes.stream()
        .map(node -> node.cloneForAttachment(aNodeFromTargetCluster))
        .reduce(
            new Stripe(),
            Stripe::addNode,
            (s1, s2) -> new Stripe().addNodes(s1.getNodes()).addNodes(s2.getNodes()));
  }

  @JsonIgnore
  public int getNodeCount() {
    return nodes.size();
  }

  public Optional<Node> getNode(String nodeName) {
    return nodes.stream().filter(node -> node.getNodeName().equals(nodeName)).findFirst();
  }
}
