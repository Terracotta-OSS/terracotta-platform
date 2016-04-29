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

package org.terracotta.management.service.monitoring;

import org.terracotta.monitoring.IMonitoringProducer;

import java.io.PrintStream;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.terracotta.management.service.monitoring.Mutation.Type.ADDITION;
import static org.terracotta.management.service.monitoring.Mutation.Type.CHANGE;
import static org.terracotta.management.service.monitoring.Mutation.Type.REMOVAL;

/**
 * @author Mathieu Carbou
 */
class MonitoringService {

  private static final Logger LOGGER = Logger.getLogger(MonitoringService.class.getName());

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private static final Queue<Mutation> EMPTY_QUEUE = new LinkedList<>();

  private final Node tree = new Node();
  private final MonitoringServiceConfiguration config;
  private final ConcurrentMap<Long, Queue<Mutation>> mutations = new ConcurrentHashMap<>();
  private final Clock clock;

  MonitoringService(Clock clock, MonitoringServiceConfiguration config) {
    this.config = config;
    this.clock = clock;
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
    };
  }

  IMonitoringConsumer getConsumer(long callerConsumerID, MonitoringConsumerConfiguration configuration) {

    // when a new consumer is requested, create a consumer queue for itself to stock all mutations that happened
    if (configuration.isRecordingMutations()) {
      mutations.computeIfAbsent(callerConsumerID, this::buildConsumerQueue);
    }

    return new IMonitoringConsumer() {
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
      public Stream<Mutation> readMutations() {
        return MonitoringService.this.readMutations(callerConsumerID);
      }

      @Override
      public void close() {
        mutations.remove(callerConsumerID);
      }
    };
  }

  // cleanup

  void clear() {
    mutations.clear();
    tree.removeChildren();
  }

  /// producing

  private boolean addNode(long callerConsumerID, String[] parents, String name, Object value) {
    if (parents == null) {
      parents = new String[0];
    }

    Node parent = getNodeForPath(parents);
    if (parent == null) {
      return false;
    }

    Node child = new Node(value);
    Node previous = parent.addChild(name, child);

    if (config.isDebug()) {
      PrintStream writer = System.out;
      writer.println("addNode() " + String.join("/", parents) + (parents.length > 0 ? "/" : "") + name);
      dumpTree(tree, 0, writer);
    }

    if (previous == null) {
      // addition, value can be null (=> would be a branch addition)
      recordMutation(new TreeMutation(clock.millis(), ADDITION, parents, name, null, value, getParentValues(parents)));
      return true;
    }

    if (previous.equals(child)) {
      // same leaf => no mutations, node added (see IMonitoringProducer contract)
      return true;
    }

    // first record child removal for this node, since a replacement removes children
    recordChildRemoval(parents, name, previous);

    // the nodes have the same name, check if their values are different
    Mutation mutation = new TreeMutation(clock.millis(), CHANGE, parents, name, previous.value, value, getParentValues(parents));
    if (mutation.isValueChanged()) {
      recordMutation(mutation);
    }

    return true;
  }

  private boolean removeNode(long consumerID, String[] parents, String name) {
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
    recordMutation(new TreeMutation(clock.millis(), REMOVAL, parents, name, removed.value, null, getParentValues(parents)));
    return true;
  }

  private void dumpTree(Node parent, int level, PrintStream writer) {
    int indent = 4;
    String prefix = "";
    for (int i = 0; i < level * indent; i++) {
      prefix += " ";
    }
    for (Map.Entry<String, Node> entry : parent.children.entrySet()) {
      writer.println(prefix + "- " + entry.getKey() + " : " + entry.getValue().value);
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
          recordMutation(new TreeMutation(clock.millis(), REMOVAL, fullPath, childName, removed.value, null, parentValues));
        }
      }
    }
  }

  private void recordMutation(Mutation mutation) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.finest("recordMutation: " + mutation);
    }
    // when a mutation is recorded, we add it to all the consumer queues
    // if the queue is a BoundedEvictingPriorityQueue, older mutations will be discarded
    for (Queue<Mutation> queue : mutations.values()) {
      queue.offer(mutation);
    }
  }

  // consuming

  private Stream<Mutation> readMutations(long callerConsumerID) {
    return StreamSupport.stream(new MutationSpliterator(callerConsumerID), false);
  }

  private Queue<Mutation> buildConsumerQueue(long callerConsumerID) {
    return new BoundedEvictingPriorityQueue<>(
        config.getMaximumUnreadMutationsPerConsumer(),
        mutation -> {
          if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Discarded mutation " + mutation + " from consumer " + callerConsumerID);
          }
        });
  }

  private <T> Optional<T> getValueForNode(long callerConsumerID, String[] path, Class<T> type) throws ClassCastException {
    Node node = getNodeForPath(path);
    if (null != node) {
      try {
        return Optional.ofNullable(type.cast(node.value));
      } catch (ClassCastException e) {
        // We catch this here to add more information and re-throw a new exception.
        String nodePath = String.join("/", path);
        String requestedType = type.getName() + " (loaded by: " + type.getClassLoader() + ")";
        Class<?> valueClass = node.value.getClass();
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
      values[i] = node.value;
    }
    return values;
  }

  // inner classes

  private static class Node {

    private final Object value;
    private final ConcurrentMap<String, Node> children = new ConcurrentHashMap<>(0);

    Node() {
      this(null);
    }

    Node(Object value) {
      this.value = value;
    }

    Node addChild(String name, Node child) {
      return this.children.put(name, child);
    }

    void removeChildren() {
      children.clear();
    }

    Node removeChild(String name) {
      return this.children.remove(name);
    }

    Node getChild(String name) {
      return this.children.get(name);
    }

    Set<String> getChildNames() {
      return this.children.keySet();
    }

    Map<String, Object> getChildValues() {
      Map<String, Object> copy = new HashMap<>(children.size());
      for (Map.Entry<String, Node> entry : this.children.entrySet()) {
        copy.put(entry.getKey(), entry.getValue().value);
      }
      return copy;
    }

    @Override
    public String toString() {
      return String.valueOf(value) + " " + getChildNames();
    }

    boolean hasChild() {
      return !children.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Node node = (Node) o;

      if (value != null ? !value.equals(node.value) : node.value != null) return false;
      return children.equals(node.children);

    }

    @Override
    public int hashCode() {
      int result = value != null ? value.hashCode() : 0;
      result = 31 * result + children.hashCode();
      return result;
    }
  }

  private class MutationSpliterator extends Spliterators.AbstractSpliterator<Mutation> {
    private final long callerConsumerID;

    MutationSpliterator(long callerConsumerID) {
      super(mutations.getOrDefault(callerConsumerID, EMPTY_QUEUE).size(), NONNULL | ORDERED | SIZED | SUBSIZED | DISTINCT | IMMUTABLE);
      this.callerConsumerID = callerConsumerID;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Mutation> action) {
      Queue<Mutation> queue = mutations.get(callerConsumerID);
      if (queue == null) {
        return false;
      }
      Mutation mutation = queue.poll();
      if (mutation == null) {
        return false;
      }
      action.accept(mutation);
      return true;
    }
  }

}
