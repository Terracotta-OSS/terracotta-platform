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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
  private static final String MUTATIONS_CATEGORY = "monitoring-tree-mutations";

  private final Node tree = new Node();
  private final MonitoringServiceConfiguration config;
  private final ConcurrentMap<Long, ConcurrentMap<String, ReadWriteBuffer<?>>> consumers = new ConcurrentHashMap<>();
  private final SequenceGenerator generator;

  MonitoringService(MonitoringServiceConfiguration config, SequenceGenerator generator) {
    this.config = config;
    this.generator = generator;
  }

  IMonitoringProducer getProducer(long callerConsumerID) {
    return new IMonitoringProducer() {
      @Override
      public boolean addNode(String[] parents, String name, Serializable value) {
        synchronized (tree) {
          if (parents == null) {
            parents = new String[0];
          }

          Node parentNode = getNodeForPath(parents);
          if (parentNode == null) {
            return false;
          }

          MonitoringService.this.addNode(parentNode, parents, name, value);

          if (config.isDebug()) {
            PrintStream writer = System.out;
            writer.println("addNode() " + String.join("/", parents) + (parents.length > 0 ? "/" : "") + name);
            dumpTree(tree, 0, writer);
          }

          return true;
        }
      }

      @Override
      public boolean removeNode(String[] parents, String name) {
        synchronized (tree) {
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
      }

      @Override
      public void pushBestEffortsData(String category, Serializable data) {
        push(category, data);
      }
    };
  }

  IMonitoringConsumer getConsumer(long callerConsumerID) {
    consumers.put(callerConsumerID, new ConcurrentHashMap<>());
    return new IMonitoringConsumer() {
      @Override
      public Optional<Collection<String>> getChildNamesForNode(String[] path) {
        Set<String> children = null;
        Node node = getNodeForPath(path);
        if (null != node) {
          children = node.getChildNames();
        }
        return Optional.ofNullable(children);
      }

      @Override
      public <T> Optional<T> getValueForNode(String[] path, Class<T> type) throws ClassCastException {
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
      public Optional<Map<String, Object>> getChildValuesForNode(String[] path) {
        Map<String, Object> children = null;
        Node node = getNodeForPath(path);
        if (null != node) {
          children = node.getChildValues();
        }
        return Optional.ofNullable(children);
      }

      @SuppressWarnings("unchecked")
      @Override
      public <V> ReadOnlyBuffer<V> getOrCreateBestEffortBuffer(String category, int maxBufferSize, Class<V> type) {
        if (MUTATIONS_CATEGORY.equals(category) && Mutation.class != type) {
          throw new IllegalArgumentException("Protected buffer name: " + MUTATIONS_CATEGORY);
        }
        return new TypedReadWriteBuffer<>(consumers.get(callerConsumerID).computeIfAbsent(category, s -> new RingBuffer<V>(maxBufferSize)), type);
      }

      @Override
      public void close() {
        consumers.remove(callerConsumerID);
      }
    };
  }

  // cleanup

  void clear() {
    consumers.clear();
    tree.removeChildren();
  }

  /// producing

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

  @SuppressWarnings("unchecked")
  private void push(String category, Object data) {
    for (ConcurrentMap<String, ReadWriteBuffer<?>> buffers : consumers.values()) {
      ReadWriteBuffer buffer = buffers.get(category);
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

  private void recordMutation(Mutation.Type type, String[] parents, String name, Object oldValue, Object newValue, Object[] parentValues) {
    TreeMutation mutation = new TreeMutation(generator.next(), type, parents, name, oldValue, newValue, parentValues);
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.finest("recordMutation: " + mutation);
    }
    // when a mutation is recorded, we add it to all the consumer's ring buffers
    push("monitoring-tree-mutations", mutation);
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
