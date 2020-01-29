/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.startup;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.service.ConfigChangeHandlerManagerImpl;
import org.terracotta.dynamic_config.server.service.ParameterSubstitutor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.testing.ExceptionMatcher.throwing;

public class StartupManagerTest {
  private static final StartupManager STARTUP_MANAGER = new StartupManager(new ParameterSubstitutor(), new ConfigChangeHandlerManagerImpl());
  private static final String CONFIG_FILE = "/path/to/config-file";
  private static final IParameterSubstitutor PARAMETER_SUBSTITUTOR = new ParameterSubstitutor();

  @Rule
  public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

  @Test
  public void testConfigFileContainsOneNode_noNodeHostPortSpecified() {
    Node node = Node.newDefaultNode("localhost");
    Cluster cluster = new Cluster(new Stripe(node));
    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFile(null, null, CONFIG_FILE, cluster);

    assertThat(matchingNode, equalTo(node));
    assertThat(systemOutRule.getLog(), containsString("Found only one node information in config file"));
  }

  @Test
  public void testConfigFileContainsOneNode_matchingNodeHostPortSpecified() {
    Node node = Node.newDefaultNode(PARAMETER_SUBSTITUTOR.substitute(Setting.NODE_HOSTNAME.getDefaultValue()));
    Cluster cluster = new Cluster(new Stripe(node));
    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFile(node.getNodeHostname(), String.valueOf(node.getNodePort()), CONFIG_FILE, cluster);

    assertThat(matchingNode, equalTo(node));
    assertThat(systemOutRule.getLog(), containsString("Found matching node entry in config file"));
  }

  @Test
  public void testConfigFileContainsOneNode_noMatchingNodeHostPortSpecified() {
    Node node = Node.newDefaultNode("localhost");
    Cluster cluster = new Cluster(new Stripe(node));

    assertThat(
        () -> STARTUP_MANAGER.getMatchingNodeFromConfigFile("blah", "12345", CONFIG_FILE, cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("Did not find a matching node entry in config file")))
    );
  }

  @Test
  public void testConfigFileContainsMultipleNodes_noNodeHostPortSpecified_foundMatchUsingDefaults() {
    Node node1 = Node.newDefaultNode(PARAMETER_SUBSTITUTOR.substitute(Setting.NODE_HOSTNAME.getDefaultValue()));
    Node node2 = Node.newDefaultNode("localhost", 1234);
    Cluster cluster = new Cluster(new Stripe(node1, node2));

    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFile(null, null, CONFIG_FILE, cluster);
    assertThat(matchingNode, equalTo(node1));
    assertThat(systemOutRule.getLog(), containsString("Found matching node entry in config file"));
  }

  @Test
  public void testConfigFileContainsMultipleNodes_noNodeHostPortSpecified() {
    Node node1 = Node.newDefaultNode("some-host", 1234);
    Node node2 = Node.newDefaultNode("some-host", 5678);
    Cluster cluster = new Cluster(new Stripe(node1, node2));

    assertThat(
        () -> STARTUP_MANAGER.getMatchingNodeFromConfigFile(null, null, CONFIG_FILE, cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("Did not find a matching node entry in config file")))
    );
  }

  @Test
  public void testConfigFileContainsMultipleNodes_matchingNodeHostPortSpecified() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost", 1234);
    Cluster cluster = new Cluster(new Stripe(node1, node2));
    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFile(node2.getNodeHostname(), String.valueOf(node2.getNodePort()), CONFIG_FILE, cluster);

    assertThat(matchingNode, equalTo(node2));
    assertThat(systemOutRule.getLog(), containsString("Found matching node entry in config file"));
  }

  @Test
  public void testConfigFileContainsMultipleNodes_noMatchingNodeHostPortSpecified() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost", 1234);
    Cluster cluster = new Cluster(new Stripe(node1, node2));

    assertThat(
        () -> STARTUP_MANAGER.getMatchingNodeFromConfigFile("blah", null, CONFIG_FILE, cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("Did not find a matching node entry in config file")))
    );
  }
}