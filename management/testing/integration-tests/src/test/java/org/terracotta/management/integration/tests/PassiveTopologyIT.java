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
package org.terracotta.management.integration.tests;

import org.junit.Test;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;

import java.io.File;
import java.io.FileWriter;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
public class PassiveTopologyIT extends AbstractHATest {

  @Test
  public void topology_includes_passives() throws Exception {
    Cluster cluster = tmsAgentService.readTopology();
    Server passive = cluster.serverStream().filter(server -> !server.isActive()).findFirst().get();
    final String[] currentPassive = {toJson(passive.toMap()).toString()};
    cluster.clientStream().forEach(client -> currentPassive[0] = currentPassive[0]
        .replace(passive.getServerName(), "stripe-PASSIVE"));

    String actual = removeRandomValues(currentPassive[0]);

    // and compare
    assertEquals(readJson("passive.json").toString(), actual);
  }

}
