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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
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

  public boolean containsNode(InetSocketAddress address) {
    return nodes.stream().map(Node::getNodeAddress).anyMatch(isEqual(address));
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Stripe clone() {
    return new Stripe(nodes.stream().map(Node::clone).collect(toList()));
  }

  public boolean detach(InetSocketAddress address) {
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
  public void attach(Node source) {
    if (containsNode(source.getNodeAddress())) {
      throw new IllegalArgumentException("Node " + source.getNodeAddress() + " is already in the stripe");
    }

    if (isEmpty()) {
      throw new IllegalStateException("Empty stripe");
    }

    Node aNode = nodes.iterator().next();

    // override all the cluster-wide parameters of the node to be attached
    source
        .setSecurityAuthc(aNode.getSecurityAuthc())
        .setSecuritySslTls(aNode.isSecuritySslTls())
        .setSecurityWhitelist(aNode.isSecurityWhitelist())
        .setFailoverPriority(aNode.getFailoverPriority())
        .setClientReconnectWindow(aNode.getClientReconnectWindow())
        .setClientLeaseDuration(aNode.getClientLeaseDuration())
        .setOffheapResources(aNode.getOffheapResources());

    // validate security folder
    if (aNode.getSecurityDir() != null && source.getSecurityDir() == null) {
      throw new IllegalArgumentException("Node " + source.getNodeAddress() + " must be started with a security directory");
    }
    if (aNode.getSecurityDir() == null && source.getSecurityDir() != null) {
      // node was started with a security directory but destination cluster is not secured so we do not need one
      source.setSecurityDir(null);
    }

    // Validate the user data directories.
    // We validate that the node we want to attach has EXACTLY the same user data directories ID as the destination cluster.
    Set<String> requiredDataDirs = aNode.getDataDirs().keySet();
    Set<String> dataDirs = source.getDataDirs().keySet();
    if (!dataDirs.containsAll(requiredDataDirs)) {
      // case where the attached node would not have all the required IDs
      requiredDataDirs.removeAll(dataDirs);
      throw new IllegalArgumentException("Node " + source.getNodeAddress() + " must declare the following data directories: " + String.join(", ", new TreeSet<>(requiredDataDirs)));
    }
    if (dataDirs.size() > requiredDataDirs.size()) {
      // case where the attached node would have more than the required IDs
      dataDirs.removeAll(requiredDataDirs);
      throw new IllegalArgumentException("Node " + source.getNodeAddress() + " must not declare the following data directories: " + String.join(", ", new TreeSet<>(dataDirs)));
    }

    nodes.add(source);
  }
}
