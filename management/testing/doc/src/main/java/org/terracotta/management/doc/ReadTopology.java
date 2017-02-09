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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.management.entity.nms.client.NmsService;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.cluster.Cluster;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class ReadTopology {
  public static void main(String[] args) throws ConnectionException, EntityConfigurationException, IOException {
    String className = ReadTopology.class.getSimpleName();

    Connection connection = Utils.createConnection(className, args.length == 1 ? args[0] : "terracotta://localhost:9510");
    NmsService service = Utils.createNmsService(connection, className);

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    mapper.addMixIn(CapabilityContext.class, CapabilityContextMixin.class);

    ScheduledFuture<?> task = executorService.scheduleWithFixedDelay(() -> {
      try {

        // READ TOPOLOGY
        Cluster cluster = service.readTopology();
        System.out.println(mapper.writeValueAsString(cluster.toMap()));

      } catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
        LoggerFactory.getLogger(className).error("ERR: " + e.getMessage(), e);
      }
    }, 0, 5, TimeUnit.SECONDS);

    System.in.read();

    task.cancel(false);
    executorService.shutdown();
    connection.close();
  }

  public static abstract class CapabilityContextMixin {
    @JsonIgnore
    public abstract Collection<String> getRequiredAttributeNames();

    @JsonIgnore
    public abstract Collection<CapabilityContext.Attribute> getRequiredAttributes();
  }

}
