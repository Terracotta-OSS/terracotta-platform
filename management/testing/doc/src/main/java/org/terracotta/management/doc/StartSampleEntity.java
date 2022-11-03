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

import org.terracotta.connection.ConnectionException;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.client.CacheFactory;

import java.net.URI;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class StartSampleEntity {
  public static void main(String[] args) throws ConnectionException, ExecutionException, TimeoutException, InterruptedException {
    CacheFactory cacheFactory = new CacheFactory(UUID.randomUUID().toString(), URI.create("terracotta://localhost:9510"), "pet-clinic");

    cacheFactory.init();

    Cache pets = cacheFactory.getCache("pets");

    while (true) {

      String key = "pet-" + new Random().nextInt(100);
      System.out.println("put(" + key + ")");
      pets.put(key, "Garfield");

      key = "pet-" + new Random().nextInt(100);
      System.out.println("get(" + key + ")");
      pets.get(key);

      Thread.sleep(1_000);
    }

  }
}
