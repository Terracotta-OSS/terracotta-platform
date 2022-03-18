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
package org.terracotta.dynamic_config.api.model;

import java.util.Arrays;

import static java.util.Collections.emptyList;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;

/**
 * This methods are only for testing purpose to create some model objects quick
 *
 * @author Mathieu Carbou
 */
public class Testing {
  public static Cluster newTestCluster(String name, Stripe... stripes) {
    return new Cluster(Arrays.asList(stripes))
        .setFailoverPriority(availability())
        .setName(name);
  }

  public static Cluster newTestCluster() {
    return newTestCluster((String) null);
  }

  public static Cluster newTestCluster(String name) {
    return new Cluster(emptyList())
        .setFailoverPriority(availability())
        .setName(name);
  }

  public static Cluster newTestCluster(Stripe... stripes) {
    return new Cluster(stripes)
        .setFailoverPriority(availability());
  }

  public static Node newTestNode(String name, String hostname, int port) {
    return new Node()
        .setName(name)
        .setPort(port)
        .setHostname(hostname);
  }

  public static Node newTestNode(String name, String hostname) {
    return new Node()
        .setName(name)
        .setHostname(hostname);
  }
}
