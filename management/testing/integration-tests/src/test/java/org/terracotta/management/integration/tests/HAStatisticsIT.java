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
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Mathieu Carbou
 */
public class HAStatisticsIT extends AbstractHATest {

  @Test
  public void can_do_remote_management_calls_on_servers() throws Exception {
    try {
      triggerServerStatComputation();

      Set<String> servers = new HashSet<>();

      queryAllRemoteStatsUntil(stats -> {

        for (ContextualStatistics stat : stats) {
          String serverName = stat.getContext().get(Server.NAME_KEY);
          servers.add(serverName);
        }

        // counters are ok, we ensure we got them from both servers
        return servers.size() == 2;
      });
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  @Test
  public void test_passive_stats() throws Exception {
    try {
      triggerServerStatComputation();

      //System.out.println("put(pet1=Cubitus)");
      put(0, "pets", "pet1", "Cubitus"); // put on both active and passive

      Set<String> servers = new HashSet<>();

      queryAllRemoteStatsUntil(stats -> {

        // do another put
        put(0, "pets", "pet1", "Cubitus"); // put on both active and passive

        for (ContextualStatistics stat : stats) {
          // only consider stats for pet-clinic/pets
          if(stat.getContext().get("alias").equals("pet-clinic/pets")) {
            String serverName = stat.getContext().get(Server.NAME_KEY);
            //System.out.println("server: " + serverName);

            Number counter = stat.getStatistic("Cluster:PutCount");
            //System.out.println("counterHistory: " + counterHistory.getLast().getValue());

            // if the counter history is not yet 1, return false, we continue looping
            if (counter.longValue()< 1L) {
              return false;

            } else {
              // to be sure we receive stats from 2 servers
              servers.add(serverName);
            }
          }
        }

        // counters are ok, we ensure we got them from both servers
        return servers.size() == 2;
      });
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

}
