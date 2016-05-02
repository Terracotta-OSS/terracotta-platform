/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.terracotta.management.model.cluster;

import org.terracotta.management.model.context.Context;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public final class Cluster implements Contextual, Serializable {

  private final ConcurrentMap<String, Client> clients = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Stripe> stripes = new ConcurrentHashMap<>();

  private Cluster() {
  }

  public Stream<Client> clientStream() {
    return clients.values().stream();
  }

  public Stream<Stripe> stripeStream() {
    return stripes.values().stream();
  }

  public Map<String, Client> getClients() {
    return Collections.unmodifiableMap(clients);
  }

  public int getClientCount() {
    return clients.size();
  }

  public Map<String, Stripe> getStripes() {
    return Collections.unmodifiableMap(stripes);
  }

  public int getStripeCount() {
    return stripes.size();
  }

  public Cluster addClient(Client client) {
    for (Client c : clients.values()) {
      if (c.getClientIdentifier().equals(client.getClientIdentifier())) {
        throw new IllegalArgumentException("Duplicate client: " + client.getClientIdentifier());
      }
    }
    if (clients.putIfAbsent(client.getId(), client) != null) {
      throw new IllegalArgumentException("Duplicate client: " + client.getId());
    } else {
      client.setParent(this);
    }
    return this;
  }

  public Optional<Client> getClient(Context context) {
    return getClient(context.get(Client.KEY));
  }

  public Optional<Client> getClient(ClientIdentifier clientIdentifier) {
    return clientStream().filter(client -> client.getClientIdentifier().equals(clientIdentifier)).findFirst();
  }

  public Optional<Client> getClient(String id) {
    return id == null ? Optional.empty() : Optional.ofNullable(clients.get(id));
  }

  public Optional<Client> removeClient(String id) {
    Optional<Client> client = getClient(id);
    client.ifPresent(c -> {
      if (clients.remove(id, c)) {
        c.detach();
      }
    });
    return client;
  }

  public Cluster addStripe(Stripe stripe) {
    if (stripes.putIfAbsent(stripe.getId(), stripe) != null) {
      throw new IllegalArgumentException("Duplicate stripe: " + stripe.getId());
    } else {
      stripe.setParent(this);
    }
    return this;
  }

  public Optional<Stripe> getStripe(Context context) {
    return getStripe(context.get(Stripe.KEY));
  }

  public Optional<Stripe> getStripe(String id) {
    return id == null ? Optional.empty() : Optional.ofNullable(stripes.get(id));
  }

  public Optional<Stripe> removeStripe(String id) {
    Optional<Stripe> stripe = getStripe(id);
    stripe.ifPresent(s -> {
      if (stripes.remove(id, s)) {
        s.detach();
      }
    });
    return stripe;
  }

  public Optional<Manageable> getActiveManageable(Context context) {
    Optional<Manageable> manageable = getStripe(context).flatMap(s -> s.getActiveManageable(context));
    if (manageable.isPresent()) {
      return manageable;
    }
    return getClient(context).flatMap(c -> c.getManageable(context));
  }

  public Optional<Server> getServer(Context context) {
    return getStripe(context).flatMap(s -> s.getServer(context));
  }

  public List<? extends Node> getNodes(Context context) {
    List<Node> nodes = new LinkedList<>();
    getStripe(context).ifPresent(stripe1 -> {
      nodes.add(stripe1);
      stripe1.getServer(context).ifPresent(server -> {
        nodes.add(server);
        server.getManageable(context).ifPresent(nodes::add);
      });
    });
    getClient(context).ifPresent(client -> {
      nodes.add(client);
      client.getManageable(context).ifPresent(nodes::add);
    });
    return nodes;
  }

  public String getPath(Context context) {
    List<? extends Node> nodes = getNodes(context);
    StringBuilder sb = new StringBuilder(nodes.isEmpty() ? "" : nodes.get(0).getId());
    for (int i = 1; i < nodes.size(); i++) {
      sb.append("/").append(nodes.get(i));
    }
    return sb.toString();
  }

  public Stream<Manageable> activeManageableStream() {
    return Stream.concat(activeServerManageableStream(), clientManageableStream());
  }

  public Stream<Manageable> activeServerManageableStream() {
    return stripeStream().flatMap(Stripe::activeManageableStream);
  }

  public Stream<Manageable> clientManageableStream() {
    return clientStream().flatMap(Client::manageableStream);
  }

  public Stream<Server> serverStream() {
    return stripeStream().flatMap(Stripe::serverStream);
  }

  @Override
  public Context getContext() {
    return Context.empty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Cluster cluster = (Cluster) o;

    if (!clients.equals(cluster.clients)) return false;
    return stripes.equals(cluster.stripes);

  }

  @Override
  public int hashCode() {
    int result = clients.hashCode();
    result = 31 * result + stripes.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return toMap().toString();
  }

  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put("stripes", stripeStream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(Stripe::toMap).collect(Collectors.toList()));
    map.put("clients", clientStream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(Client::toMap).collect(Collectors.toList()));
    return map;
  }

  public static Cluster create() {
    return new Cluster();
  }

}
