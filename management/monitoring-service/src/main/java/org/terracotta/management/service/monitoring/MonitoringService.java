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

import org.terracotta.management.sequence.SequenceGenerator;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.terracotta.management.service.monitoring.Mutation.Type.ADDITION;
import static org.terracotta.management.service.monitoring.Mutation.Type.CHANGE;
import static org.terracotta.management.service.monitoring.Mutation.Type.REMOVAL;

/**
 * @author Mathieu Carbou
 */
class MonitoringService {

  private static final Logger LOGGER = Logger.getLogger(MonitoringService.class.getName());

  private static final AtomicLong MUTATION_INDEX = new AtomicLong(Long.MIN_VALUE);

  private final Node tree = new Node();
  private final MonitoringServiceConfiguration config;
  private final ConcurrentMap<MonitoringConsumer, ConcurrentMap<String, ReadWriteBuffer<?>>> consumers = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, MonitoringConsumer> categories = new ConcurrentHashMap<>();
  private final SequenceGenerator generator;

  MonitoringService(MonitoringServiceConfiguration config, SequenceGenerator generator) {
    this.config = config;
    this.generator = generator;
  }

  IMonitoringProducer getProducer(long callerConsumerID) {
    return new IMonitoringProducer() {
      @Override
      public boolean addNode(String[] parents, String name, Object value) {
        return MonitoringService.this.addNode(callerConsumerID, parents, name, value);
      }

      @Override
      public boolean removeNode(String[] parents, String name) {
        return MonitoringService.this.removeNode(callerConsumerID, parents, name);
      }

      @Override
      public void pushBestEffortsData(String category, Object data) {
        MonitoringService.this.pushBestEffortsData(callerConsumerID, category, data);
      }
    };
  }

  IMonitoringConsumer getConsumer(long callerConsumerID, MonitoringConsumerConfiguration configuration) {

    MonitoringConsumer consumer = new MonitoringConsumer(callerConsumerID, configuration) {
      @Override
      public Optional<Collection<String>> getChildNamesForNode(String[] path) {
        return MonitoringService.this.getChildNamesForNode(callerConsumerID, path);
      }

      @Override
      public <T> Optional<T> getValueForNode(String[] path, Class<T> type) throws ClassCastException {
        return MonitoringService.this.getValueForNode(callerConsumerID, path, type);
      }

      @Override
      public Optional<Map<String, Object>> getChildValuesForNode(String[] path) {
        return MonitoringService.this.getChildValuesForNode(callerConsumerID, path);
      }

      @Override
      public <V> ReadOnlyBuffer<V> getOrCreateBestEffortBuffer(String category, int maxBufferSize, Class<V> type) {
        return MonitoringService.this.getOrCreateBestEffortBuffer(this, category, maxBufferSize, type);
      }

      @Override
      public void close() {
        Map<String, ReadWriteBuffer<?>> buffers = consumers.remove(this);
        if (buffers != null) {
          buffers.forEach((k, v) -> categories.remove(k, this));
        }
      }
    };

    consumers.put(consumer, new ConcurrentHashMap<>());

    return consumer;
  }

  // cleanup

  void clear() {
    consumers.clear();
    categories.clear();
    tree.removeChildren();
  }

  /// producing

  private synchronized boolean addNode(long callerConsumerID, String[] parents, String name, Object value) {
    if (parents == null) {
      parents = new String[0];
    }

    Node parentNode = getNodeForPath(parents);
    if (parentNode == null) {
      return false;
    }

    addNode(parentNode, parents, name, value);

    if (config.isDebug()) {
      PrintStream writer = System.out;
      writer.println("addNode() " + String.join("/", parents) + (parents.length > 0 ? "/" : "") + name);
      dumpTree(tree, 0, writer);
    }

    return true;
  }

  private void addNode(Node parentNode, String[] parents, String name, Object value) {
    Node child = new Node(value);
    Node previous = parentNode.addChild(name, child);

    if (previous == null) {
      // addition, value can be null (=> would be a branch addition)
      recordMutation(ADDITION, parents, name, null, value, getParentValues(parents));
      return;
    }

    if (previous.equals(child)) {
      // same leaf => no mutations, node added (see IMonitoringProducer contract)
      return;
    }

    // first record child removal for this node, since a replacement removes children
    recordChildRemoval(parents, name, previous);

    // the nodes have the same name, check if their values are different
    if (!Objects.equals(previous.getValue(), value)) {
      recordMutation(CHANGE, parents, name, previous.getValue(), value, getParentValues(parents));
    }
  }

  private synchronized boolean removeNode(long consumerID, String[] parents, String name) {
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

    if (config.isDebug()) {
      PrintStream writer = System.out;
      writer.println("removeNode() " + String.join("/", parents) + (parents.length > 0 ? "/" : "") + name);
      dumpTree(tree, 0, writer);
    }

    recordChildRemoval(parents, name, removed);
    recordMutation(REMOVAL, parents, name, removed.getValue(), null, getParentValues(parents));
    return true;
  }

  private void pushBestEffortsData(long callerConsumerID, String category, Object data) {
    IMonitoringConsumer key = categories.get(category);
    if (key != null) {
      ReadWriteBuffer buffer = consumers.get(key).get(category);
      if (buffer != null) {
        buffer.put(data);
      }
    }
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

  // mutations

  private void recordChildRemoval(String[] parents, String name, Node node) {
    while (node.hasChild()) {
      String[] fullPath = Utils.concat(parents, name);
      Object[] parentValues = getParentValues(parents);

      for (String childName : node.getChildNames()) {
        Node removed = node.removeChild(childName);
        if (removed != null) {
          recordChildRemoval(fullPath, childName, removed);
          recordMutation(REMOVAL, fullPath, childName, removed.getValue(), null, parentValues);
        }
      }
    }
  }

  private synchronized void recordMutation(Mutation.Type type, String[] parents, String name, Object oldValue, Object newValue, Object[] parentValues) {
    TreeMutation mutation = new TreeMutation(generator.next(), type, parents, name, oldValue, newValue, parentValues);
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.finest("recordMutation: " + mutation);
    }
    // when a mutation is recorded, we add it to all the consumer's ring buffers
    for (MonitoringConsumer consumer : consumers.keySet()) {
      consumer.record(mutation);
    }
  }

  // consuming

  private <T> Optional<T> getValueForNode(long callerConsumerID, String[] path, Class<T> type) throws ClassCastException {
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

  private Optional<Map<String, Object>> getChildValuesForNode(long callerConsumerID, String[] path) {
    Map<String, Object> children = null;
    Node node = getNodeForPath(path);
    if (null != node) {
      children = node.getChildValues();
    }
    return Optional.ofNullable(children);
  }

  private Optional<Collection<String>> getChildNamesForNode(long callerConsumerID, String[] path) {
    Set<String> children = null;
    Node node = getNodeForPath(path);
    if (null != node) {
      children = node.getChildNames();
    }
    return Optional.ofNullable(children);
  }

  private <V> ReadOnlyBuffer<V> getOrCreateBestEffortBuffer(MonitoringConsumer consumer, String category, int maxBufferSize, Class<V> type) {
    MonitoringConsumer monitoringConsumer = categories.computeIfAbsent(category, s -> consumer);
    if (monitoringConsumer != consumer) {
      throw new IllegalArgumentException("Buffer on category " + category + " is already hold by consumer " + consumer);
    }
    return (ReadWriteBuffer<V>) consumers.get(monitoringConsumer).computeIfAbsent(category, s -> new RingBuffer<V>(maxBufferSize) {
      @Override
      public synchronized void put(V value) {
        if (!type.isInstance(value)) {
          throw new IllegalArgumentException("Value type is " + value.getClass() + ". Required type is " + type);
        }
        super.put(value);
      }
    });
  }

  private Node getNodeForPath(String[] path) {
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

  private Object[] getParentValues(String[] path) {
    Object[] values = new Object[path.length];
    Arrays.fill(values, null);
    Node node = this.tree;
    for (int i = 0; i < path.length; i++) {
      node = node.getChild(path[i]);
      // Make sure we didn't fall off.
      if (null == node) {
        break;
      }
      values[i] = node.getValue();
    }
    return values;
  }

}
