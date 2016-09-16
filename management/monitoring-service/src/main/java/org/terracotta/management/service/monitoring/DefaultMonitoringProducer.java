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
package org.terracotta.management.service.monitoring;

import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.terracotta.management.service.monitoring.DefaultMonitoringConsumer.SERVERS_PATH;

/**
 * @author Mathieu Carbou
 */
class DefaultMonitoringProducer implements MonitoringTree, IStripeMonitoring {

  private final Node tree = new Node();
  private final long consumerId;
  private final Map<Long, DefaultMonitoringConsumer> consumers;

  protected PlatformServer currentActiveServer;

  DefaultMonitoringProducer(long consumerId, Map<Long, DefaultMonitoringConsumer> consumers) {
    this.consumerId = consumerId;
    this.consumers = consumers;
  }

  @Override
  public void serverDidBecomeActive(PlatformServer server) {
    this.currentActiveServer = server;
  }

  @Override
  public void serverDidJoinStripe(PlatformServer server) {
    addNode(server, SERVERS_PATH, server.getServerName(), server);
  }

  @Override
  public void serverDidLeaveStripe(PlatformServer server) {
    removeNode(server, SERVERS_PATH, server.getServerName());
  }

  @Override
  public long getConsumerId() {
    return consumerId;
  }

  @Override
  public void pushBestEffortsData(PlatformServer server, String name, Serializable data) {
    for (DefaultMonitoringConsumer consumer : consumers.values()) {
      consumer.push(name, data);
    }
  }

  @Override
  public <T extends Serializable> Optional<T> getValueForNode(String[] path, Class<T> type) throws ClassCastException {
    Node node = getNodeForPath(path);
    if (null != node) {
      try {
        return Optional.ofNullable(type.cast(node.getValue()));
      } catch (ClassCastException e) {
        // We catch this here to add more information and re-throw a new exception.
        String nodePath = String.join("/", path);
        String requestedType = type.getName() + " (loaded by: " + type.getClassLoader() + ")";
        Class<?> valueClass = node.getValue().getClass();
        String actualType = valueClass.getName() + " (loaded by: " + valueClass.getClassLoader() + ")";
        String message = nodePath + " type is " + actualType + " but requested " + requestedType;
        throw new ClassCastException(message);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Collection<String>> getChildNamesForNode(String... path) {
    Set<String> children = null;
    Node node = getNodeForPath(path);
    if (null != node) {
      children = node.getChildNames();
    }
    return Optional.ofNullable(children);
  }

  @Override
  public Optional<Map<String, Serializable>> getChildValuesForNode(String... path) {
    Map<String, Serializable> children = null;
    Node node = getNodeForPath(path);
    if (null != node) {
      children = node.getChildValues();
    }
    return Optional.ofNullable(children);
  }

  @Override
  public boolean containsPath(String... path) {
    return getNodeForPath(path) != null;
  }

  @Override
  public synchronized boolean addNode(PlatformServer server, String[] parents, String name, Serializable value) {
    if (parents == null) {
      parents = new String[0];
    }

    Node parentNode = getNodeForPath(parents);
    if (parentNode == null) {
      return false;
    }

    parentNode.addChild(name, new Node(value));

    if (Config.DEBUG) {
      PrintStream writer = System.out;
      writer.println("[" + consumerId + "] addNode() " + String.join("/", parents) + (parents.length > 0 ? "/" : "") + name);
      dumpTree(tree, 0, writer);
    }

    return true;
  }

  @Override
  public synchronized boolean removeNode(PlatformServer server, String[] parents, String name) {
    if (parents == null) {
      parents = new String[0];
    }

    Node parent = getNodeForPath(parents);
    if (parent == null) {
      return false;
    }

    Node removed = parent.removeChild(name);
    if (removed == null) {
      return false;
    }

    if (Config.DEBUG) {
      PrintStream writer = System.out;
      writer.println("[" + consumerId + "] removeNode() " + String.join("/", parents) + (parents.length > 0 ? "/" : "") + name);
      dumpTree(tree, 0, writer);
    }

    return true;
  }

  private Node getNodeForPath(String... path) {
    Node node = this.tree;
    for (String name : path) {
      node = node.getChild(name);
      // Make sure we didn't fall off.
      if (null == node) {
        break;
      }
    }
    return node;
  }

  private void dumpTree(Node parent, int level, PrintStream writer) {
    int indent = 4;
    String prefix = "";
    for (int i = 0; i < level * indent; i++) {
      prefix += " ";
    }
    for (Map.Entry<String, Node> entry : parent.getChildren().entrySet()) {
      writer.println(prefix + "- " + entry.getKey() + " : " + (entry.getValue().getValue() == null ? null : (entry.getValue().getValue().getClass().isArray() ? Arrays.deepToString((Object[]) entry.getValue().getValue()) : entry.getValue().getValue())));
      dumpTree(entry.getValue(), level + 1, writer);
    }
  }

  protected String dumpTree() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream writer = new PrintStream(baos);
    dumpTree(tree, 0, writer);
    try {
      return baos.toString("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("MonitoringTree{");
    sb.append("consumerId=").append(consumerId);
    sb.append('}');
    return sb.toString();
  }

}
