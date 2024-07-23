/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.stream.IntStream.rangeClosed;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;

/**
 * These methods are only for testing purpose to create some model objects quick
 *
 * @author Mathieu Carbou
 */
public class Testing {

  // some UIDs that never change over time
  @SuppressFBWarnings("MS_PKGPROTECT")
  public static final UID A_UID = UID.newUID(new Random(0));
  @SuppressFBWarnings("MS_PKGPROTECT")
  public static final UID[] C_UIDS = IntStream.range(0, 10).mapToObj(idx -> UID.newUID(new Random(idx))).toArray(UID[]::new);
  @SuppressFBWarnings("MS_PKGPROTECT")
  public static final UID[] S_UIDS = IntStream.range(10, 20).mapToObj(idx -> UID.newUID(new Random(idx))).toArray(UID[]::new);
  @SuppressFBWarnings("MS_PKGPROTECT")
  public static final UID[] N_UIDS = IntStream.range(20, 30).mapToObj(idx -> UID.newUID(new Random(idx))).toArray(UID[]::new);

  public static Cluster newTestCluster(String name, Stripe... stripes) {
    return new Cluster(Arrays.asList(stripes))
        .setUID(C_UIDS[0])
        .setFailoverPriority(availability())
        .setName(name);
  }

  public static Cluster newTestCluster() {
    return newTestCluster((String) null);
  }

  public static Cluster newTestCluster(String name) {
    return new Cluster(emptyList())
        .setUID(C_UIDS[0])
        .setFailoverPriority(availability())
        .setName(name);
  }

  public static Cluster newTestCluster(Stripe... stripes) {
    return new Cluster(stripes)
        .setUID(C_UIDS[0])
        .setFailoverPriority(availability());
  }

  public static Stripe newTestStripe(String name) {
    return newTestStripe(name, S_UIDS[1]);
  }

  public static Stripe newTestStripe(String name, UID uid) {
    return new Stripe()
        .setUID(uid)
        .setName(name);
  }

  public static Node newTestNode(String name, String hostname, int port) {
    return newTestNode(name, hostname, port, N_UIDS[1]);
  }

  public static Node newTestNode(String name, String hostname) {
    return newTestNode(name, hostname, N_UIDS[1]);
  }

  public static Node newTestNode(String name, String hostname, int port, UID uid) {
    return new Node()
        .setUID(uid)
        .setName(name)
        .setPort(port)
        .setHostname(hostname);
  }

  public static Node newTestNode(String name, String hostname, UID uid) {
    return new Node()
        .setUID(uid)
        .setName(name)
        .setHostname(hostname);
  }

  public static void replaceUIDs(Cluster cluster) {
    if (cluster.getUID() != null) {
      cluster.setUID(C_UIDS[0]);
    }
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripes().get(stripeId - 1);
      if (stripe.getUID() != null) {
        stripe.setUID(S_UIDS[stripeId]);
      }
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        Node node = stripe.getNodes().get(nodeId - 1);
        if (node.getUID() != null) {
          node.setUID(N_UIDS[stripeId * cluster.getStripeCount() + nodeId]);
        }
      });
    });
  }

  public static void replaceRequiredUIDs(Cluster cluster, UID uid) {
    if (cluster.getUID() != null) {
      cluster.setUID(uid);
    }
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripes().get(stripeId - 1);
      if (stripe.getUID() != null) {
        stripe.setUID(uid);
      }
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        Node node = stripe.getNodes().get(nodeId - 1);
        if (node.getUID() != null) {
          node.setUID(uid);
        }
      });
    });
  }

  public static void replaceRequiredNames(Cluster cluster, String name) {
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripes().get(stripeId - 1);
      if (stripe.getName() != null) {
        stripe.setName(name);
      }
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        Node node = stripe.getNodes().get(nodeId - 1);
        if (node.getName() != null) {
          node.setName(name);
        }
      });
    });
  }

  public static void resetRequiredUIDs(Cluster cluster, UID uid) {
    cluster.setUID(uid);
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripes().get(stripeId - 1);
      stripe.setUID(uid);
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        Node node = stripe.getNodes().get(nodeId - 1);
        node.setUID(uid);
      });
    });
  }

  public static void resetRequiredNames(Cluster cluster, String name) {
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripes().get(stripeId - 1);
      stripe.setName(name);
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        Node node = stripe.getNodes().get(nodeId - 1);
        node.setName(name);
      });
    });
  }

}
