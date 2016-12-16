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

    // removes all random values

    cluster.serverStream().forEach(server -> {
      server.setActivateTime(0);
      server.setUpTimeSec(0);
      server.setStartTime(0);
      server.setBuildId("Build ID");
      server.setVersion("");
      server.setGroupPort(0);
      server.setBindPort(0);
    });

    cluster.serverEntityStream()
        .map(ServerEntity::getManagementRegistry)
        .flatMap(managementRegistry -> Stream.of(
            managementRegistry.flatMap(r -> r.getCapability("ServerCacheSettings")),
            managementRegistry.flatMap(r -> r.getCapability("OffHeapResourceSettings"))))
        .forEach(capability -> {
          if (capability.isPresent()) {
            capability.get()
                .getDescriptors(Settings.class)
                .stream()
                .filter(settings -> settings.containsKey("time")).forEach(settings -> settings.set("time", 0));
          }
        });

    Server passive = cluster.serverStream().filter(server -> !server.isActive()).findFirst().get();
    final String[] currentPassive = {toJson(passive.toMap()).toString()};
    cluster.clientStream().forEach(client -> currentPassive[0] = currentPassive[0]
        .replace(passive.getServerName(), "stripe-PASSIVE"));

    // please leave this: easy to compare if something changes
    /*FileWriter w = new FileWriter(new File("target/out.json"));
    w.write(currentPassive[0]);
    w.close();*/

    // and compare
    assertEquals(readJson("passive.json").toString(), currentPassive[0]);
  }

}
