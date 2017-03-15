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
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
public class CollectorReschedulingIT extends AbstractSingleTest {

  @Test
  public void can_reschedule_client_collectors() throws Exception {
    triggerClientStatComputation(1, TimeUnit.SECONDS);

    long start, end;

    do {
      start = System.currentTimeMillis();
      readClientStatistics();
      end = System.currentTimeMillis();
    } while (end - start < 1000 && !Thread.currentThread().isInterrupted());

    triggerClientStatComputation(4, TimeUnit.SECONDS);

    do {
      start = System.currentTimeMillis();
      readClientStatistics();
      end = System.currentTimeMillis();
    } while (end - start < 2000 && !Thread.currentThread().isInterrupted());
  }

  @Test
  public void can_reschedule_server_collectors() throws Exception {
    triggerServerStatComputation(1, TimeUnit.SECONDS);

    long start, end;

    do {
      start = System.currentTimeMillis();
      readServerStatistics();
      end = System.currentTimeMillis();
    } while (end - start < 1000 && !Thread.currentThread().isInterrupted());

    triggerServerStatComputation(4, TimeUnit.SECONDS);

    do {
      start = System.currentTimeMillis();
      readServerStatistics();
      end = System.currentTimeMillis();
    } while (end - start < 2000 && !Thread.currentThread().isInterrupted());
  }

  private List<? extends ContextualStatistics> readClientStatistics() throws InterruptedException, ExecutionException, TimeoutException {
    List<? extends ContextualStatistics> statistics;
    do {
      statistics = nmsService.readMessages()
          .stream()
          .filter(message -> message.getType().equals("STATISTICS"))
          .flatMap(message -> message.unwrap(ContextualStatistics.class).stream())
          .filter(contextualStatistics -> contextualStatistics.getContext().getOrDefault(Client.KEY, "").contains("pet-clinic"))
          .collect(Collectors.toList());
    } while (statistics.isEmpty() && !Thread.currentThread().isInterrupted());
    return statistics;
  }

  private List<? extends ContextualStatistics> readServerStatistics() throws InterruptedException, ExecutionException, TimeoutException {
    List<? extends ContextualStatistics> statistics;
    do {
      statistics = nmsService.readMessages()
          .stream()
          .filter(message -> message.getType().equals("STATISTICS"))
          .flatMap(message -> message.unwrap(ContextualStatistics.class).stream())
          .filter(contextualStatistics -> contextualStatistics.getContext().contains(Server.KEY))
          .collect(Collectors.toList());
    } while (statistics.isEmpty() && !Thread.currentThread().isInterrupted());
    return statistics;
  }

}