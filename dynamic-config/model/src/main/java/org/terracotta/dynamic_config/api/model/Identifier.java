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

import org.terracotta.inet.InetSocketAddressConverter;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class Identifier {

  private final String identifier;

  @Override
  public String toString() {
    return identifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Identifier)) return false;
    Identifier that = (Identifier) o;
    return identifier.equals(that.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier);
  }

  private Identifier(String identifier) {
    this.identifier = requireNonNull(identifier);
  }

  public static Identifier valueOf(String identifier) {
    return new Identifier(identifier);
  }

  /**
   * Find an object based on this identifier, based on the type.
   * <p>
   * An identifier can be a UID, a name, an address.
   * <p>
   * type can be a stripe or node or a cluster
   * <p>
   * If node is requested, we try to find the object in the topology matching this
   * identifier
   * <p>
   * If stripe is requested, we try to find the stripe in the topology matching this
   * identifier, or containing a node matching this identifier
   * <p>
   * If cluster is requested, we ensure the cluster contains such identifier for the
   * cluster object, a stripe object or a node object
   */
  public Optional<? extends PropertyHolder> findObject(Cluster cluster, Scope type) {

    // UID ?
    UID uid;
    try {
      uid = UID.valueOf(identifier);
    } catch (RuntimeException e) {
      uid = null;
    }

    InetSocketAddress address;
    try {
      address = InetSocketAddressConverter.getInetSocketAddress(identifier);
    } catch (RuntimeException e) {
      address = null;
    }

    switch (type) {

      case NODE: {
        return findNode(cluster.getNodes(), uid, address);
      }

      case STRIPE: {
        return findStripe(cluster.getStripes(), uid, address);
      }

      case CLUSTER: {
        // ensure this cluster contains the identifier
        return identifier.equals(cluster.getName())
            || cluster.getUID().equals(uid)
            || findStripe(cluster.getStripes(), uid, address).isPresent() ?
            Optional.of(cluster) :
            Optional.empty();
      }

      default: {
        throw new AssertionError(type);
      }
    }
  }

  private Optional<Stripe> findStripe(Collection<Stripe> stripes, UID uid, InetSocketAddress address) {
    for (Stripe stripe : stripes) {
      if (stripe.getName().equals(identifier) || stripe.getUID().equals(uid)) {
        return Optional.of(stripe);
      }
    }
    // not found ? try to search for a node within this stripe
    for (Stripe stripe : stripes) {
      Optional<Node> node = findNode(stripe.getNodes(), uid, address);
      if (node.isPresent()) {
        return Optional.of(stripe);
      }
    }
    return Optional.empty();
  }

  private Optional<Node> findNode(Collection<Node> nodes, UID uid, InetSocketAddress address) {
    for (Node node : nodes) {
      if (node.getName().equals(identifier)
          || node.getUID().equals(uid)
          || node.getInternalSocketAddress().equals(address)
          || node.getPublicSocketAddress().isPresent() && node.getPublicSocketAddress().get().equals(address)) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }
}
