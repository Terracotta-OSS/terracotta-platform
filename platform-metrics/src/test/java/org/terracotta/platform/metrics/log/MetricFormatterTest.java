/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.platform.metrics.log;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.platform.metrics.platform.PlatformMetricService;

import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MetricFormatter.
 *
 * <p>Log line format:
 * <pre>
 *   STATS:{unix}:{seconds}|T:{trigger}|{topology}|P:...|S:...|V:...|C:...|F:...|EIC:...|CIC:...|SIC:...|QIC:...
 * </pre>
 * Field indices when split on {@code |}: [0]=header, [1]=T:trigger, [2]=topology,
 * [3]=P, [4]=S, [5]=V, [6]=C, [7]=F, [8]=EIC, [9]=CIC, [10]=SIC, [11]=QIC.
 */
public class MetricFormatterTest {

  private MetricFormatter formatter;
  private PlatformMetricService service;

  @Before
  public void setUp() {
    formatter = new MetricFormatter();
    service = mock(PlatformMetricService.class);
    // Every test needs a non-null stripe-states map or format() will NPE on .values().stream()
    when(service.getStripeStates()).thenReturn(new TreeMap<>());
  }

  // --- header format ---

  @Test
  public void testHeaderContainsMarkerTimestampAndSeconds() {
    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1700000000L, 300L, service);
    assertThat(line, startsWith("STATS:1700000000:300|"));
  }

  @Test
  public void testHeaderDifferentTimestampAndSeconds() {
    String line = formatter.format(MetricLoggingTrigger.TOPOLOGY_CHANGE, 1700000000L, 123L, service);
    assertThat(line, startsWith("STATS:1700000000:123|"));
  }

  // --- trigger field ---

  @Test
  public void testTriggerFieldPeriodicCode() {
    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1700000000L, 300L, service);
    // trigger field is [1] after splitting on |
    String triggerField = line.split("\\|")[1];
    assertEquals("T:P", triggerField);
  }

  @Test
  public void testTriggerFieldTopologyChangeCode() {
    String line = formatter.format(MetricLoggingTrigger.TOPOLOGY_CHANGE, 1L, 1L, service);
    String triggerField = line.split("\\|")[1];
    assertEquals("T:T", triggerField);
  }

  @Test
  public void testTriggerFieldOnDemandCode() {
    String line = formatter.format(MetricLoggingTrigger.ON_DEMAND, 1L, 1L, service);
    String triggerField = line.split("\\|")[1];
    assertEquals("T:D", triggerField);
  }

  // --- field structure ---

  @Test
  public void testLineContainsPipeDelimitedFields() {
    when(service.getCurrentConnectionCount()).thenReturn(5L);
    when(service.getMeteredConnectionOpened(true)).thenReturn(3L);
    when(service.getMeteredConnectionClosed(true)).thenReturn(1L);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    assertTrue("Line must have pipe delimiters", line.contains("|"));
    assertTrue("Line must contain P: field", line.contains("|P:"));
  }

  @Test
  public void testAllExpectedFieldsPresent() {
    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    assertTrue(line.contains("|T:"));
    assertTrue(line.contains("|P:"));
    assertTrue(line.contains("|S:"));
    assertTrue(line.contains("|V:"));
    assertTrue(line.contains("|C:"));
    assertTrue(line.contains("|F:"));
    assertTrue(line.contains("|EIC:"));
    assertTrue(line.contains("|CIC:"));
    assertTrue(line.contains("|SIC:"));
    assertTrue(line.contains("|QIC:"));
  }

  // --- gauge formatting ---

  @Test
  public void testGaugeFieldFormatWithMinimumWidth() {
    when(service.getCurrentConnectionCount()).thenReturn(5L);
    when(service.getMeteredConnectionOpened(true)).thenReturn(3L);
    when(service.getMeteredConnectionClosed(true)).thenReturn(1L);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    // gauge: 4-digit minimum (0005), delta: 3 chars total incl. sign (+03, -01)
    assertTrue("P field gauge format", line.contains("|P:0005,"));
    assertTrue("P field open delta format", line.contains("+03"));
    assertTrue("P field close delta format", line.contains("-01"));
  }

  @Test
  public void testGaugeFieldZeroValues() {
    // all defaults are 0; deltaWidth=3 → "+00" and "-00" (sign + 2 digits)
    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    assertTrue("P field zero gauge", line.contains("|P:0000,"));
    assertTrue("P delta zero format", line.contains("+00"));
    assertTrue("C field zero gauge", line.contains("|C:0000,"));
  }

  @Test
  public void testGaugeWidthExpandsForLargeValues() {
    when(service.getCurrentConnectionCount()).thenReturn(12345L);
    when(service.getMeteredConnectionOpened(true)).thenReturn(0L);
    when(service.getMeteredConnectionClosed(true)).thenReturn(0L);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    // 12345 = 5 digits, exceeds min width of 4
    assertTrue("P gauge should show full value", line.contains("|P:12345,"));
  }

  @Test
  public void testDeltaWidthExpandsForLargeDelta() {
    when(service.getMeteredConnectionOpened(true)).thenReturn(1234L);
    when(service.getMeteredConnectionClosed(true)).thenReturn(0L);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    // +1234 = 5 chars (sign + 4 digits), exceeds min delta width of 3
    assertTrue("P delta should show full value", line.contains("+1234"));
  }

  // --- meter-only field formatting ---

  @Test
  public void testMeterOnlyFieldEIC() {
    when(service.getMeteredTotalEntityInvocations(true)).thenReturn(567L);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    assertTrue("EIC field format", line.contains("|EIC:+567"));
  }

  @Test
  public void testMeterOnlyFieldZero() {
    // MIN_DELTA_WIDTH=3 → "+00" (sign + 2 digits)
    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    assertTrue("EIC zero format", line.contains("|EIC:+00"));
    assertTrue("CIC zero format", line.contains("|CIC:+00"));
    assertTrue("SIC zero format", line.contains("|SIC:+00"));
    assertTrue("QIC zero format", line.contains("|QIC:+00"));
  }

  // --- field ordering ---

  @Test
  public void testFieldOrder() {
    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);
    // header | T | topology | P | S | V | C | F | EIC | CIC | SIC | QIC
    int tIdx   = line.indexOf("|T:");
    int pIdx   = line.indexOf("|P:");
    int sIdx   = line.indexOf("|S:");
    int vIdx   = line.indexOf("|V:");
    int cIdx   = line.indexOf("|C:");
    int fIdx   = line.indexOf("|F:");
    int eicIdx = line.indexOf("|EIC:");
    int cicIdx = line.indexOf("|CIC:");
    int sicIdx = line.indexOf("|SIC:");
    int qicIdx = line.indexOf("|QIC:");

    assertTrue(tIdx   < pIdx);
    assertTrue(pIdx   < sIdx);
    assertTrue(sIdx   < vIdx);
    assertTrue(vIdx   < cIdx);
    assertTrue(cIdx   < fIdx);
    assertTrue(fIdx   < eicIdx);
    assertTrue(eicIdx < cicIdx);
    assertTrue(cicIdx < sicIdx);
    assertTrue(sicIdx < qicIdx);
  }

  // --- counter sign prefix ---

  @Test
  public void testNegativeDeltaHasMinusPrefix() {
    when(service.getCurrentConnectionCount()).thenReturn(2L);
    when(service.getMeteredConnectionOpened(true)).thenReturn(0L);
    when(service.getMeteredConnectionClosed(true)).thenReturn(5L);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    // close delta must have '-' prefix (e.g. "-05" or "-5")
    assertTrue("close delta must have - prefix", line.contains("-0") || line.contains("-5"));
  }

  @Test
  public void testPositiveDeltaHasPlusPrefix() {
    when(service.getMeteredConnectionOpened(true)).thenReturn(7L);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    assertTrue("open delta must have + prefix", line.contains("+0") || line.contains("+7"));
  }

  // --- topology field ---

  @Test
  public void testTopologyFieldEmptyWhenNoStates() {
    // getStripeStates() returns empty map → topology segment is an empty string at field [2]
    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    // Line: STATS:...|T:P|<topology>|P:...
    String[] parts = line.split("\\|", 4);
    // parts[2] is the topology segment — empty map produces an empty string
    assertEquals("topology segment must be empty for no states", "", parts[2]);
  }

  @Test
  public void testTopologyFieldSingleActiveServer() {
    TreeMap<String, LogicalServerState> states = new TreeMap<>();
    states.put("node1:9410", LogicalServerState.ACTIVE);
    when(service.getStripeStates()).thenReturn(states);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    // ACTIVE maps to "A"; topology is field [2]
    String topology = line.split("\\|", 4)[2];
    assertEquals("A", topology);
  }

  @Test
  public void testTopologyFieldActiveAndPassive() {
    TreeMap<String, LogicalServerState> states = new TreeMap<>();
    // TreeMap orders by key: node1 < node2 alphabetically
    states.put("node1:9410", LogicalServerState.ACTIVE);
    states.put("node2:9410", LogicalServerState.PASSIVE);
    when(service.getStripeStates()).thenReturn(states);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    String topology = line.split("\\|", 4)[2];
    assertEquals("A,P", topology);
  }

  @Test
  public void testTopologyFieldAllKnownStateCodes() {
    // Verify each LogicalServerState maps to a non-null, non-empty code
    TreeMap<String, LogicalServerState> states = new TreeMap<>();
    LogicalServerState[] allStates = LogicalServerState.values();
    for (int i = 0; i < allStates.length; i++) {
      states.put("node" + i + ":9410", allStates[i]);
    }
    when(service.getStripeStates()).thenReturn(states);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    String topology = line.split("\\|", 4)[2];
    assertFalse("topology must not be empty when states are present", topology.isEmpty());
    // Each code is comma-separated; count must match number of states
    String[] codes = topology.split(",");
    assertEquals(allStates.length, codes.length);
    for (String code : codes) {
      assertFalse("each state code must be non-empty", code.isEmpty());
    }
  }

  @Test
  public void testTopologyFieldSynchronizingState() {
    TreeMap<String, LogicalServerState> states = new TreeMap<>();
    states.put("node1:9410", LogicalServerState.SYNCHRONIZING);
    when(service.getStripeStates()).thenReturn(states);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    String topology = line.split("\\|", 4)[2];
    assertEquals("PS", topology);
  }

  @Test
  public void testTopologyFieldBootstrapRecoveringAndStoppingStates() {
    TreeMap<String, LogicalServerState> states = new TreeMap<>();
    states.put("node1:9410", LogicalServerState.BOOTSTRAP);
    states.put("node2:9410", LogicalServerState.RECOVERING);
    states.put("node3:9410", LogicalServerState.STOPPING);
    when(service.getStripeStates()).thenReturn(states);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    String topology = line.split("\\|", 4)[2];
    assertEquals("B,C,?", topology);
  }

  @Test
  public void testTopologyFieldUndeterminedStatesMapToQuestionMark() {
    TreeMap<String, LogicalServerState> states = new TreeMap<>();
    states.put("node1:9410", LogicalServerState.UNINITIALIZED);
    states.put("node2:9410", LogicalServerState.STOPPING);
    states.put("node3:9410", LogicalServerState.UNREACHABLE);
    states.put("node4:9410", LogicalServerState.UNKNOWN);
    when(service.getStripeStates()).thenReturn(states);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1L, 300L, service);

    String topology = line.split("\\|", 4)[2];
    assertEquals("?,?,?,?", topology);
  }

  // --- parse examples from design doc ---

  @Test
  public void testDesignDocExampleCanBeExtractedWithFieldSplit() {
    when(service.getCurrentVoterCount()).thenReturn(1L);
    when(service.getMeteredVoterJoins(true)).thenReturn(3L);
    when(service.getMeteredVoterLeaves(true)).thenReturn(2L);
    when(service.getCurrentClientCount()).thenReturn(10L);
    when(service.getMeteredClientJoins(true)).thenReturn(3L);
    when(service.getMeteredClientLeaves(true)).thenReturn(2L);

    String line = formatter.format(MetricLoggingTrigger.PERIODIC, 1700000000L, 300L, service);

    // Field order: [0]=header, [1]=T:trigger, [2]=topology, [3]=P, [4]=S, [5]=V, [6]=C, [7]=F, [8]=EIC, ...
    String[] fields = line.split("\\|");
    assertEquals("V field label", "V", fields[5].split(":")[0]);
    assertEquals("C field label", "C", fields[6].split(":")[0]);

    // Voter current value (deltaWidth adapts to max("3".length+1, "2".length+1) = 2 → max(3,2)=3 chars total incl. sign)
    String vField = fields[5]; // "V:0001,+03,-02"
    String[] vParts = vField.split("[:,]");
    assertEquals("voter current", "0001", vParts[1]);
    assertEquals("voter +delta", "+03", vParts[2]);
    assertEquals("voter -delta", "-02", vParts[3]);
  }
}
