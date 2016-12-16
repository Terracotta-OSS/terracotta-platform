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
package org.terracotta.management.entity.sample;

import org.junit.Test;
import org.terracotta.management.entity.sample.client.CacheFactory;

import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class CacheEntityFeaturesTest extends AbstractTest {

  @Test
  public void cache_remains_active_on_server_on_client_close() throws Exception {
    CacheFactory cacheFactory = new CacheFactory(URI.create("passthrough://stripe-1:9510/cat-clinic"));
    cacheFactory.init();
    Cache cache = cacheFactory.getCache("cache");
    cache.put("client1", "Mat");
    cacheFactory.close();

    cacheFactory = new CacheFactory(URI.create("passthrough://stripe-1:9510/cat-clinic"));
    cacheFactory.init();
    cache = cacheFactory.getCache("cache");
    try {
      assertThat(cache.get("client1"), equalTo("Mat"));
    } finally {
      cacheFactory.close();
    }
  }

  @Test
  public void puts_can_be_seen_on_other_clients() throws Exception {
    put(0, "clients", "client1", "Mathieu");

    assertThat(get(0, "clients", "client1"), equalTo("Mathieu"));
    assertThat(get(1, "clients", "client1"), equalTo("Mathieu"));

    put(1, "clients", "client2", "Anthony");

    assertThat(get(0, "clients", "client2"), equalTo("Anthony"));
    assertThat(get(1, "clients", "client2"), equalTo("Anthony"));
  }

  @Test
  public void replaces_can_be_seen_on_other_clients() throws Exception {
    put(0, "clients", "client1", "Mathieu");
    put(1, "clients", "client1", "Anthony");

    assertThat(get(0, "clients", "client1"), equalTo("Anthony"));
    assertThat(get(1, "clients", "client1"), equalTo("Anthony"));
  }

  @Test
  public void removes_can_be_seen_on_other_clients() throws Exception {
    put(0, "clients", "client1", "Mathieu");
    remove(1, "clients", "client1");
    assertThat(size(0, "clients"), equalTo(0));
    assertThat(size(1, "clients"), equalTo(0));
    assertThat(get(0, "clients", "client1"), is(nullValue()));
    assertThat(get(1, "clients", "client1"), is(nullValue()));
  }

  @Test
  public void removes_are_fired_to_local_cache() throws Exception {
    put(0, "clients", "client1", "Mathieu");

    // put in local cache
    assertThat(get(0, "clients", "client1"), equalTo("Mathieu"));
    assertThat(get(1, "clients", "client1"), equalTo("Mathieu"));

    remove(1, "clients", "client1");

    assertThat(size(0, "clients"), equalTo(0));
    assertThat(size(1, "clients"), equalTo(0));

    assertThat(get(0, "clients", "client1"), is(nullValue()));
    assertThat(get(1, "clients", "client1"), is(nullValue()));
  }

  @Test
  public void size_reports_each_layer_size() throws Exception {
    assertThat(size(0, "clients"), equalTo(0));
    assertThat(size(1, "clients"), equalTo(0));

    put(0, "clients", "client1", "Mathieu");
    assertThat(size(0, "clients"), equalTo(0));
    assertThat(size(1, "clients"), equalTo(0));

    assertThat(get(1, "clients", "client1"), equalTo("Mathieu"));
    assertThat(size(0, "clients"), equalTo(0));
    assertThat(size(1, "clients"), equalTo(1));

    assertThat(get(0, "clients", "client1"), equalTo("Mathieu"));
    assertThat(size(0, "clients"), equalTo(1));
    assertThat(size(1, "clients"), equalTo(1));
  }

  @Test
  public void clear_is_local_and_empty_the_heap() throws Exception {
    put(0, "clients", "client1", "Mathieu");

    assertThat(size(0, "clients"), equalTo(0));
    assertThat(size(1, "clients"), equalTo(0));

    assertThat(get(1, "clients", "client1"), equalTo("Mathieu"));
    assertThat(get(0, "clients", "client1"), equalTo("Mathieu"));

    assertThat(size(0, "clients"), equalTo(1));
    assertThat(size(1, "clients"), equalTo(1));

    caches.get("clients").get(0).clear();
    caches.get("clients").get(1).clear();

    assertThat(size(0, "clients"), equalTo(0));
    assertThat(size(1, "clients"), equalTo(0));

    assertThat(get(1, "clients", "client1"), equalTo("Mathieu"));
    assertThat(get(0, "clients", "client1"), equalTo("Mathieu"));
  }

  @Test(expected = NullPointerException.class)
  public void no_null_keys_on_get() throws Exception {
    caches.get("clients").get(0).get(null);
  }

  @Test(expected = NullPointerException.class)
  public void no_null_keys_on_remove() throws Exception {
    caches.get("clients").get(0).remove(null);
  }

  @Test(expected = NullPointerException.class)
  public void no_null_keys_on_put() throws Exception {
    caches.get("clients").get(0).put(null, "");
  }

  @Test(expected = NullPointerException.class)
  public void no_null_value_on_put() throws Exception {
    caches.get("clients").get(0).put("k", null);
  }

}
