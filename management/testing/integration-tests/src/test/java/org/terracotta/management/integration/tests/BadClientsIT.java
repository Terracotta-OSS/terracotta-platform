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
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.client.CacheFactory;
import org.terracotta.management.entity.sample.client.ClientCache;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class BadClientsIT extends AbstractSingleTest {

  @Test
  public void two_clients_with_same_identifiers_do_not_crash_the_server() throws Exception {
    URI uri = cluster.getConnectionURI();
    String uuid = UUID.randomUUID().toString();

    CacheFactory cacheFactory1 = new CacheFactory(nextInstanceId(), uri, "bad-client");
    cacheFactory1.init(uuid);
    Cache foo1 = cacheFactory1.getCache("foo");

    long count = nmsService.readTopology().clientStream()
        .filter(client -> client.getClientIdentifier().getConnectionUid().equals(uuid))
        .count();
    assertThat(count, equalTo(1L));

    // create another client that will have the same client identifier
    CacheFactory cacheFactory2 = new CacheFactory(nextInstanceId(), uri, "bad-client");
    cacheFactory2.init(uuid);
    ClientCache foo2 = (ClientCache) cacheFactory1.getCache("foo");

    count = nmsService.readTopology().clientStream()
        .filter(client -> client.getClientIdentifier().getConnectionUid().equals(uuid))
        .count();
    assertThat(count, equalTo(1L));

    count = nmsService.readTopology().clientStream()
        .filter(client -> client.getClientIdentifier().getName().equals("bad-client"))
        .count();
    assertThat(count, equalTo(1L));

    foo1.put("key", "val");
    assertThat(foo2.get("key"), equalTo("val"));

    foo2.close();
    cacheFactory2.getConnection().close();
    
    do {
      count = nmsService.readTopology().clientStream()
          .filter(client -> client.getClientIdentifier().getConnectionUid().equals(uuid))
          .count();
    } while (!Thread.currentThread().isInterrupted() && count != 0);
    assertThat(count, equalTo(0L));

    cacheFactory1.getConnection().close();
  }

}
