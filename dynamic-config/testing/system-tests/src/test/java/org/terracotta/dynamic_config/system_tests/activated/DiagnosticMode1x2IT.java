/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;
import org.terracotta.dynamic_config.system_tests.util.NodeOutputRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsLog;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DiagnosticMode1x2IT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_restart_active_in_diagnostic_mode() {
    int activeNodeId = findActive(1).getAsInt();
    TerracottaServer active = getNode(1, activeNodeId);
    tsa.stop(active);
    assertThat(tsa.getStopped().size(), is(1));

    startNode(active, "--diagnostic-mode", "-n", active.getServerSymbolicName().getSymbolicName(), "-r", active.getConfigRepo());
    waitUntil(out.getLog(1, activeNodeId), containsLog("Node is starting in diagnostic mode. This mode is used to manually repair a broken configuration on a node."));
    waitUntil(out.getLog(1, activeNodeId), containsLog("Started the server in diagnostic mode"));
  }

  @Test
  public void test_restart_passive_in_diagnostic_mode() {
    int passiveNodeId = findPassives(1)[0];
    TerracottaServer passive = getNode(1, passiveNodeId);
    tsa.stop(passive);
    assertThat(tsa.getStopped().size(), is(1));

    startNode(passive, "--diagnostic-mode", "-n", passive.getServerSymbolicName().getSymbolicName(), "-r", passive.getConfigRepo());
    waitUntil(out.getLog(1, passiveNodeId), containsLog("Node is starting in diagnostic mode. This mode is used to manually repair a broken configuration on a node."));
    waitUntil(out.getLog(1, passiveNodeId), containsLog("Started the server in diagnostic mode"));
  }
}
