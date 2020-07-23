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
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.isEqual;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;

/**
 * This methods are only for testing purpose to create some model objects quick
 *
 * @author Mathieu Carbou
 */
public class Testing {
  public static Cluster newTestCluster(String name, Stripe... stripes) {
    return fillDefaults(new Cluster(Arrays.asList(stripes)))
        .setFailoverPriority(availability())
        .setName(name);
  }

  public static Cluster newTestCluster() {
    return newTestCluster((String) null);
  }

  public static Cluster newTestCluster(String name) {
    return fillDefaults(new Cluster(emptyList()))
        .setFailoverPriority(availability())
        .setName(name);
  }

  public static Cluster newTestCluster(Stripe... stripes) {
    return fillDefaults(new Cluster(stripes))
        .setFailoverPriority(availability());
  }

  public static Node newTestNode(String name, String hostname, int port) {
    return fillDefaults(new Node())
        .setName(name)
        .setPort(port)
        .setHostname(hostname);
  }

  public static Node newTestNode(String hostname, int port) {
    return fillDefaults(new Node())
        .setPort(port)
        .setHostname(hostname);
  }

  public static Node newTestNode(String name, String hostname) {
    return fillDefaults(new Node())
        .setName(name)
        .setHostname(hostname);
  }

  public static Node newTestNode(String hostname) {
    return fillDefaults(new Node())
        .setHostname(hostname);
  }

  public static <T extends PropertyHolder> T fillDefaults(T o) {
    Stream.of(Setting.values())
        .filter(isEqual(Setting.NODE_HOSTNAME).negate())
        .filter(isEqual(Setting.NODE_CONFIG_DIR).negate())
        .filter(isEqual(Setting.CLUSTER_NAME).negate())
        .filter(isEqual(Setting.LICENSE_FILE).negate())
        .filter(s -> s.isScope(o.getScope()))
        .forEach(setting -> setting.fillDefault(o));
    return o;
  }
}
