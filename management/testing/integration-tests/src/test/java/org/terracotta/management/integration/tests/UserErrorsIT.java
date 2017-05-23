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
import org.terracotta.entity.EntityUserException;
import org.terracotta.management.entity.nms.client.IllegalManagementCallException;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.context.Context;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
public class UserErrorsIT extends AbstractSingleTest {

  @Test
  public void management_call_to_inexisting_client() throws Exception {
    Client client = nmsService.readTopology()
        .clientStream()
        .filter(e -> e.getName().equals("pet-clinic"))
        .findFirst()
        .get();

    // similar to cacheManagerName and cacheName context
    String badClientId = ClientIdentifier.create(1L, "127.0.0.1", "NAME", "uuid").getClientId();
    Context context = client.getContext()
        .with("clientId", badClientId)
        .with("appName", "pet-clinic")
        .with("cacheName", "pets");

    try {
      nmsService.call(context, "CacheCalls", "put", Void.TYPE, new Parameter("pet1"), new Parameter("Cat")).waitForReturn();
    } catch (TimeoutException | InterruptedException | CancellationException | IllegalManagementCallException e) {
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause(), is(instanceOf(EntityUserException.class)));
      assertThat(e.getCause().getMessage(), equalTo("Entity: org.terracotta.management.entity.nms.server.ActiveNmsServerEntity: exception in user code: java.lang.IllegalArgumentException: Client 1@127.0.0.1:NAME:uuid is either not found or not manageable"));
    }
  }

  @Test
  public void management_call_to_inexisting_entity() throws Exception {
    ServerEntity serverEntity = nmsService.readTopology()
        .activeServerEntityStream()
        .filter(e -> e.getName().equals("pet-clinic/pets"))
        .findFirst()
        .get();

    Context context = serverEntity.getContext()
        .with("cacheName", "pet-clinic/pets")
        .with(Server.NAME_KEY, "INEXISTING");

    try {
      nmsService.call(context, "ServerCacheCalls", "put", Void.TYPE, new Parameter("pet1"), new Parameter("Cat")).waitForReturn();
    } catch (TimeoutException | InterruptedException | CancellationException | IllegalManagementCallException e) {
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause(), is(instanceOf(EntityUserException.class)));
      assertThat(e.getCause().getMessage(), equalTo("Entity: org.terracotta.management.entity.nms.server.ActiveNmsServerEntity: exception in user code: java.lang.IllegalArgumentException: Server Entity {stripeId=SINGLE, serverId=testServer0, serverName=INEXISTING, entityId=pet-clinic/pets:org.terracotta.management.entity.sample.client.CacheEntity, entityName=pet-clinic/pets, entityType=org.terracotta.management.entity.sample.client.CacheEntity, consumerId=3, cacheName=pet-clinic/pets} is either not found or not manageable"));
    }
  }

}
