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
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.management.entity.nms.client.IllegalManagementCallException;
import org.terracotta.management.entity.nms.client.NmsService;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.context.Context;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class DoManagementCall {
  public static void main(String[] args) throws ConnectionException, EntityConfigurationException, IOException, InterruptedException, ExecutionException, TimeoutException, IllegalManagementCallException {
    String className = DoManagementCall.class.getSimpleName();

    Connection connection = Utils.createConnection(className, args.length == 1 ? args[0] : "terracotta://localhost:9510");
    NmsService nmsService = Utils.createNmsService(connection, className);

    Cluster cluster = nmsService.readTopology();

    // REMOTE MANAGEMENT CALL ON A CLIENT
    cluster
        .clientStream()
        .filter(c -> c.getName().startsWith("Ehcache:"))
        .forEach(client -> {

          // 2. create a routing context
          Context context = client.getContext()
              .with("cacheManagerName", "my-super-cache-manager");

          try {
            // 3. do a clear management call on a client
            nmsService.call(context, "ActionsCapability", "clear", Void.TYPE).waitForReturn();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

    // REMOTE MANAGEMENT CALL ON A SERVER
    ServerEntity serverEntity = cluster
        .activeServerEntityStream()
        .filter(e -> e.getName().equals("pet-clinic/pets"))
        .findFirst()
        .get();
    Context cacheName = serverEntity.getContext().with("cacheName", "pet-clinic/pets");
    nmsService.call(cacheName, "ServerCacheCalls", "clear", Void.TYPE).waitForReturn();

    System.in.read();

    connection.close();
  }

  public static abstract class CapabilityContextMixin {
    @JsonIgnore
    public abstract Collection<String> getRequiredAttributeNames();

    @JsonIgnore
    public abstract Collection<CapabilityContext.Attribute> getRequiredAttributes();
  }

}
