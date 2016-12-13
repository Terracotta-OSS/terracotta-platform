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
import org.terracotta.management.entity.tms.client.TmsAgentService;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class ReadNotifications {
  public static void main(String[] args) throws ConnectionException, EntityConfigurationException, IOException {
    String className = ReadNotifications.class.getSimpleName();

    Connection connection = Utils.createConnection(className, args.length == 1 ? args[0] : "terracotta://localhost:9510");
    TmsAgentService service = Utils.createTmsAgentService(connection, className);

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    ScheduledFuture<?> task = executorService.scheduleWithFixedDelay(() -> {
      try {

        List<Message> messages = service.readMessages();
        System.out.println(messages.size() + " messages");
        messages
            .stream()
            .filter(message -> message.getType().equals("NOTIFICATION"))
            .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
            .forEach(notification -> System.out.println(" - " + notification.getType() + ": " + notification.getContext() + " - " + notification.getAttributes()));

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
