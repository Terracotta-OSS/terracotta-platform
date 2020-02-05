/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2, autoStart = false)
public class AutoActivateIT extends DynamicConfigIT {
  @Test
  public void test_auto_activation_failure_for_2x1_cluster() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/2x1.properties");
    startNode(1, 1, "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort()), "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out::getLog, containsString("Unable to start a pre-activated multi-stripe cluster"));
  }

  @Test
  public void test_auto_activation_failure_for_2x2_cluster() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/2x2.properties");
    startNode(1, 1, "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort()), "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out::getLog, containsString("Unable to start a pre-activated multi-stripe cluster"));
  }

  @Test
  public void test_auto_activation_success_for_1x1_cluster() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/1x1.properties");
    startNode(1, 1, "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort()), "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void test_auto_activation_success_for_1x2_cluster() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/1x2.properties");
    startNode(1, 1, "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    startNode(1, 2, "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 2)), "--node-repository-dir", "repository/stripe1/node-2");
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
  }

  @Test
  public void test_auto_activation_failure_for_different_1x2_cluster() throws Exception {
    startNode(1, 1, "-f", copyConfigProperty("/config-property-files/1x2.properties").toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    startNode(1, 2, "-f", copyConfigProperty("/config-property-files/1x2-diff.properties").toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 2)), "--node-repository-dir", "repository/stripe1/node-2");
    waitUntil(out::getLog, containsString("Passive cannot sync because the configuration change history does not match: no match on active for this change on passive"));
  }
}