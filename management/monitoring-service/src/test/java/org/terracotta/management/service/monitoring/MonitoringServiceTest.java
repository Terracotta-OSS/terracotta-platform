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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.monitoring.IMonitoringProducer;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.terracotta.management.service.monitoring.Mutation.Type.ADDITION;
import static org.terracotta.management.service.monitoring.Mutation.Type.CHANGE;
import static org.terracotta.management.service.monitoring.Mutation.Type.REMOVAL;
import static org.terracotta.management.service.monitoring.Utils.array;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVERS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVER_STATE_STOPPED;
import static org.terracotta.monitoring.PlatformMonitoringConstants.STATE_NODE_NAME;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class MonitoringServiceTest {

  MonitoringServiceProvider serviceProvider = new MonitoringServiceProvider();
  IMonitoringProducer producer;
  IMonitoringConsumer consumer;

  @Before
  public void setUp() throws Exception {
    serviceProvider.initialize(new MonitoringServiceConfiguration().setDebug(true));
    producer = serviceProvider.getService(0, new BasicServiceConfiguration<>(IMonitoringProducer.class));
    consumer = serviceProvider.getService(0, new MonitoringConsumerConfiguration().setRecordingMutations(true));
  }

  @Test
  public void test_topology_branches() {
    // See ManagementTopologyEventCollector
    producer.addNode(new String[0], PLATFORM_ROOT_NAME, null);
    producer.addNode(PLATFORM_PATH, CLIENTS_ROOT_NAME, null);
    producer.addNode(PLATFORM_PATH, ENTITIES_ROOT_NAME, null);
    producer.addNode(PLATFORM_PATH, FETCHED_ROOT_NAME, null);
    producer.addNode(PLATFORM_PATH, SERVERS_ROOT_NAME, null);
    producer.addNode(PLATFORM_PATH, STATE_NODE_NAME, SERVER_STATE_STOPPED);

    assertEquals(SERVER_STATE_STOPPED, consumer.getValueForNode(PLATFORM_PATH, STATE_NODE_NAME, String.class).get());
    assertEquals(new HashSet<>(asList(
        CLIENTS_ROOT_NAME,
        ENTITIES_ROOT_NAME,
        FETCHED_ROOT_NAME,
        STATE_NODE_NAME,
        SERVERS_ROOT_NAME,
        STATE_NODE_NAME
    )), consumer.getChildNamesForNode(PLATFORM_PATH).get());
  }

  @Test
  public void test_inexisting_parent() {
    assertTrue(producer.addNode(null, "A", 1));
    assertFalse(producer.addNode(array("A", "AA"), "AAA", 1));
    List<Mutation> mutations = collectMutation();
    assertEquals(1, mutations.size());
    assertEquals(asList(ADDITION), types(mutations));
    assertEquals(asList("A"), paths(mutations));
    assertEquals(asList(1), vals(mutations));
    assertEquals(asList(true), changes(mutations));
  }

  @Test
  public void test_topology_mutation_add_branch_to_root() {
    assertTrue(producer.addNode(null, "A", null));
    assertTrue(producer.addNode(new String[0], "B", null));
    List<Mutation> mutations = collectMutation();
    assertEquals(2, mutations.size());
    assertEquals(asList(ADDITION, ADDITION), types(mutations));
    assertEquals(asList("A", "B"), paths(mutations));
    assertEquals(asList(null, null), vals(mutations));
    assertEquals(asList(false, false), changes(mutations));
  }

  @Test
  public void test_topology_mutation_add_leaf_to_root() {
    assertTrue(producer.addNode(null, "A", 1));
    assertTrue(producer.addNode(new String[0], "B", 2));
    List<Mutation> mutations = collectMutation();
    assertEquals(2, mutations.size());
    assertEquals(asList(ADDITION, ADDITION), types(mutations));
    assertEquals(asList("A", "B"), paths(mutations));
    assertEquals(asList(1, 2), vals(mutations));
    assertEquals(asList(true, true), changes(mutations));
  }

  @Test
  public void test_topology_mutation_add_branch_to_node() {
    assertTrue(producer.addNode(null, "A", null));
    assertTrue(producer.addNode(array("A"), "AA", null));
    List<Mutation> mutations = collectMutation();
    assertEquals(2, mutations.size());
    assertEquals(asList(ADDITION, ADDITION), types(mutations));
    assertEquals(asList("A", "A/AA"), paths(mutations));
    assertEquals(asList(null, null), vals(mutations));
    assertEquals(asList(false, false), changes(mutations));
  }

  @Test
  public void test_topology_mutation_add_leaf_to_node() {
    assertTrue(producer.addNode(null, "A", null));
    assertTrue(producer.addNode(array("A"), "AA", 11));
    List<Mutation> mutations = collectMutation();
    assertEquals(2, mutations.size());
    assertEquals(asList(ADDITION, ADDITION), types(mutations));
    assertEquals(asList("A", "A/AA"), paths(mutations));
    assertEquals(asList(null, 11), vals(mutations));
    assertEquals(asList(false, true), changes(mutations));
  }

  @Test
  public void test_topology_mutation_add_same_node() {
    assertTrue(producer.addNode(null, "A", null));
    assertTrue(producer.addNode(null, "A", null));
    List<Mutation> mutations = collectMutation();
    assertEquals(1, mutations.size());
    assertEquals(asList(ADDITION), types(mutations));
    assertEquals(asList("A"), paths(mutations));
    assertEquals(asList((Object) null), vals(mutations));
    assertEquals(asList(false), changes(mutations));
  }

  @Test
  public void test_topology_mutation_add_same_leaf() {
    assertTrue(producer.addNode(null, "A", 1));
    assertTrue(producer.addNode(null, "A", 1));
    List<Mutation> mutations = collectMutation();
    assertEquals(1, mutations.size());
    assertEquals(asList(ADDITION), types(mutations)); // only 1st insert is recorded
    assertEquals(asList("A"), paths(mutations));
    assertEquals(asList((Object) null), olds(mutations));
    assertEquals(asList(1), vals(mutations));
    assertEquals(asList(true), changes(mutations));
  }

  @Test
  public void test_topology_mutation_remove_inexisting() {
    producer.addNode(null, "A", null);
    collectMutation();

    assertFalse(producer.removeNode(null, "B"));
    assertFalse(producer.removeNode(array("B"), "AA"));
    List<Mutation> mutations = collectMutation();
    assertEquals(0, mutations.size());
  }

  @Test
  public void test_topology_mutation_remove_leaf() {
    producer.addNode(null, "A", 1);
    producer.addNode(array("A"), "AA", null);
    producer.addNode(array("A"), "AB", 1);
    collectMutation();

    assertTrue(producer.removeNode(array("A"), "AA"));
    assertTrue(producer.removeNode(array("A"), "AB"));
    List<Mutation> mutations = collectMutation();
    assertEquals(2, mutations.size());
    assertEquals(asList(REMOVAL, REMOVAL), types(mutations));
    assertEquals(asList("A/AA", "A/AB"), paths(mutations));
    assertEquals(asList(null, 1), olds(mutations));
    assertEquals(asList(false, true), changes(mutations));
    assertEquals(asList(asList(1), asList(1)), parentVals(mutations));
  }

  @Test
  public void test_topology_mutation_remove_branch_with_children() {
    producer.addNode(null, "A", 1);
    producer.addNode(array("A"), "AA", null);
    producer.addNode(array("A"), "AB", 1);
    collectMutation();

    assertTrue(producer.removeNode(null, "A"));
    List<Mutation> mutations = collectMutation();
    assertEquals(3, mutations.size());
    assertEquals(asList(REMOVAL, REMOVAL, REMOVAL), types(mutations));
    assertEquals(asList("A/AA", "A/AB", "A"), paths(mutations));
    assertEquals(asList(null, 1, 1), olds(mutations));
    assertEquals(asList(false, true, true), changes(mutations));
    assertEquals(asList(asList(), asList(), asList()), parentVals(mutations));
  }

  @Test
  public void test_topology_mutation_replace_leaf() {
    producer.addNode(null, "A", 1);
    producer.addNode(array("A"), "AA", 11);
    collectMutation();

    assertTrue(producer.addNode(array("A"), "AA", 11));
    assertTrue(producer.addNode(array("A"), "AA", 12));
    List<Mutation> mutations = collectMutation();
    assertEquals(1, mutations.size());
    assertEquals(asList(CHANGE), types(mutations));
    assertEquals(asList("A/AA"), paths(mutations));
    assertEquals(asList(11), olds(mutations));
    assertEquals(asList(12), vals(mutations));
    assertEquals(asList(true), changes(mutations));
    assertEquals(asList(asList(1)), parentVals(mutations));
  }

  @Test
  public void test_topology_mutation_replace_branch() {
    producer.addNode(null, "A", 1);
    producer.addNode(array("A"), "AA", 11);
    producer.addNode(null, "B", 2);
    producer.addNode(array("B"), "BB", 22);
    collectMutation();

    assertTrue(producer.addNode(null, "A", 1)); // same branch, child cleared
    List<Mutation> mutations = collectMutation();
    assertEquals(1, mutations.size());
    assertEquals(asList(REMOVAL), types(mutations));
    assertEquals(asList("A/AA"), paths(mutations));
    assertEquals(asList(11), olds(mutations));
    assertEquals(asList(true), changes(mutations));
    assertEquals(asList(asList()), parentVals(mutations));

    assertTrue(producer.addNode(null, "B", 333)); // branch changed, child cleared
    mutations = collectMutation();
    assertEquals(2, mutations.size());
    assertEquals(asList(REMOVAL, CHANGE), types(mutations));
    assertEquals(asList("B/BB", "B"), paths(mutations));
    assertEquals(asList(22, 2), olds(mutations));
    assertEquals(asList(null, 333), vals(mutations));
    assertEquals(asList(true, true), changes(mutations));
    assertEquals(asList(asList(), asList()), parentVals(mutations));
  }

  private List<Mutation> collectMutation() {
    return consumer.readMutations().collect(Collectors.toList());
  }

  private static List<Mutation.Type> types(List<Mutation> mutations) {
    return mutations.stream().map(Mutation::getType).collect(Collectors.toList());
  }

  private static List<String> paths(List<Mutation> mutations) {
    return mutations.stream().map(mutation -> String.join("/", mutation.getPath())).collect(Collectors.toList());
  }

  private static List<Object> vals(List<Mutation> mutations) {
    return mutations.stream().map(Mutation::getNewValue).collect(Collectors.toList());
  }

  private static List<Object> olds(List<Mutation> mutations) {
    return mutations.stream().map(Mutation::getOldValue).collect(Collectors.toList());
  }

  private static List<Boolean> changes(List<Mutation> mutations) {
    return mutations.stream().map(Mutation::isValueChanged).collect(Collectors.toList());
  }

  private static List<List<Object>> parentVals(List<Mutation> mutations) {
    return mutations.stream().map(mutation -> asList(mutation.getParentValues())).collect(Collectors.toList());
  }

}
