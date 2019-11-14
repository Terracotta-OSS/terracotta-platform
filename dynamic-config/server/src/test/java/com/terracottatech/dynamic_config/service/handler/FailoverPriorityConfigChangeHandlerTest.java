/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.handler;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.FailoverPriority;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import org.junit.Test;

import static com.terracottatech.dynamic_config.nomad.Applicability.cluster;
import static com.terracottatech.dynamic_config.util.IParameterSubstitutor.identity;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FailoverPriorityConfigChangeHandlerTest {
  private NodeContext topology = new NodeContext(new Cluster("foo", new Stripe(new Node().setNodeName("bar"))), 1, "bar");
  private SettingNomadChange set1 = SettingNomadChange.set(cluster(), Setting.FAILOVER_PRIORITY, "consistency:2");
  private SettingNomadChange set2 = SettingNomadChange.set(cluster(), Setting.FAILOVER_PRIORITY, "availability");

  @Test
  public void testTryApply() throws Exception {
    FailoverPriorityConfigChangeHandler failoverPriorityConfigChangeHandler = new FailoverPriorityConfigChangeHandler(identity());
    Cluster updatedXmlConfig = failoverPriorityConfigChangeHandler.tryApply(topology, set1.toConfiguration(topology.getCluster()));
    assertThat(updatedXmlConfig.getSingleNode().get().getFailoverPriority(), is(FailoverPriority.valueOf("consistency:2")));

    updatedXmlConfig = failoverPriorityConfigChangeHandler.tryApply(topology, set2.toConfiguration(topology.getCluster()));
    assertThat(updatedXmlConfig.getSingleNode().get().getFailoverPriority(), is(FailoverPriority.valueOf("availability")));
  }
}