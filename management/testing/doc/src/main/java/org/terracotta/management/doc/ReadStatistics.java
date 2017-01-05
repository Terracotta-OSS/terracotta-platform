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
package org.terracotta.management.doc;

import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.entity.tms.client.IllegalManagementCallException;
import org.terracotta.management.entity.tms.client.TmsAgentService;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.model.stats.Statistic;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class ReadStatistics {
  public static void main(String[] args) throws ConnectionException, EntityConfigurationException, IOException, InterruptedException, ExecutionException, TimeoutException, IllegalManagementCallException {
    String className = ReadStatistics.class.getSimpleName();

    Connection connection = Utils.createConnection(className, args.length == 1 ? args[0] : "terracotta://localhost:9510");
    TmsAgentService tmsAgentService = Utils.createTmsAgentService(connection, className);

    Cluster cluster = tmsAgentService.readTopology();

    // TRIGGER SERVER-SIDE STATS COMPUTATION

    ServerEntity serverEntity = cluster
        .activeServerEntityStream()
        .filter(e -> e.getType().equals(TmsAgentConfig.ENTITY_TYPE))
        .findFirst()
        .get();

    Context context = serverEntity.getContext();

    tmsAgentService.startStatisticCollector(context, 5, TimeUnit.SECONDS).waitForReturn();

    // TRIGGER CLIENT-SIDE STATS COMPUTATION

    cluster
        .clientStream()
        .filter(c -> c.getName().equals("pet-clinic"))
        .findFirst()
        .ifPresent(ehcache -> {

          Context ctx = ehcache.getContext()
              .with("appName", "pet-clinic");

          try {
            tmsAgentService.startStatisticCollector(ctx, 5, TimeUnit.SECONDS).waitForReturn();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    ScheduledFuture<?> task = executorService.scheduleWithFixedDelay(() -> {
      try {

        List<Message> messages = tmsAgentService.readMessages();
        System.out.println(messages.size() + " messages");
        messages
            .stream()
            .filter(message -> message.getType().equals("STATISTICS"))
            .flatMap(message -> message.unwrap(ContextualStatistics.class).stream())
            .forEach(statistics -> {
              System.out.println(statistics.getContext());
              for (Map.Entry<String, Statistic<?, ?>> entry : statistics.getStatistics().entrySet()) {
                System.out.println(" - " + entry.getKey() + "=" + entry.getValue());
              }
            });

      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        LoggerFactory.getLogger(className).error("ERR: " + e.getMessage(), e);
      }
    }, 0, 5, TimeUnit.SECONDS);

    System.in.read();

    task.cancel(false);
    executorService.shutdown();
    connection.close();
  }
}
