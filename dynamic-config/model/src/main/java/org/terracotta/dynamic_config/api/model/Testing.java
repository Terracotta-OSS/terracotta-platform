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
import static java.util.stream.IntStream.rangeClosed;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;

/**
 * This methods are only for testing purpose to create some model objects quick
 *
 * @author Mathieu Carbou
 */
public class Testing {
  public static Cluster newTestCluster(String name, Stripe... stripes) {
    return new Cluster(Arrays.asList(stripes))
        .setUID("c-uid")
        .setFailoverPriority(availability())
        .setName(name);
  }

  public static Cluster newTestCluster() {
    return newTestCluster((String) null);
  }

  public static Cluster newTestCluster(String name) {
    return new Cluster(emptyList())
        .setUID("c-uid")
        .setFailoverPriority(availability())
        .setName(name);
  }

  public static Cluster newTestCluster(Stripe... stripes) {
    return new Cluster(stripes)
        .setUID("c-uid")
        .setFailoverPriority(availability());
  }

  public static Stripe newTestStripe(String name) {
    return newTestStripe(name, "s-uid-1");
  }

  public static Stripe newTestStripe(String name, String uid) {
    return new Stripe()
        .setUID(uid)
        .setName(name);
  }

  public static Node newTestNode(String name, String hostname, int port) {
    return newTestNode(name, hostname, port, "n-uid-1");
  }

  public static Node newTestNode(String name, String hostname) {
    return newTestNode(name, hostname, "n-uid-1");
  }

  public static Node newTestNode(String name, String hostname, int port, String uid) {
    return new Node()
        .setUID(uid)
        .setName(name)
        .setPort(port)
        .setHostname(hostname);
  }

  public static Node newTestNode(String name, String hostname, String uid) {
    return new Node()
        .setUID(uid)
        .setName(name)
        .setHostname(hostname);
  }

  public static void replaceUIDs(Cluster cluster) {
    if (cluster.getUID() != null) {
      cluster.setUID("c-uid");
    }
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripe(stripeId).get();
      if (stripe.getUID() != null) {
        stripe.setUID("s-uid-" + stripeId);
      }
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        final Node node = stripe.getNode(nodeId).get();
        if (node.getUID() != null) {
          node.setUID("uid-" + stripeId + "-" + nodeId);
        }
      });
    });
  }

  public static void replaceRequiredUIDs(Cluster cluster, String placeholder) {
    if (cluster.getUID() != null) {
      cluster.setUID(placeholder);
    }
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripe(stripeId).get();
      if (stripe.getUID() != null) {
        stripe.setUID(placeholder);
      }
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        final Node node = stripe.getNode(nodeId).get();
        if (node.getUID() != null) {
          node.setUID(placeholder);
        }
      });
    });
  }

  public static void replaceRequiredNames(Cluster cluster, String placeholder) {
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripe(stripeId).get();
      if (stripe.getName() != null) {
        stripe.setName(placeholder);
      }
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        final Node node = stripe.getNode(nodeId).get();
        if (node.getName() != null) {
          node.setName(placeholder);
        }
      });
    });
  }

  public static void resetRequiredUIDs(Cluster cluster, String placeholder) {
    cluster.setUID(placeholder);
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripe(stripeId).get();
      stripe.setUID(placeholder);
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        final Node node = stripe.getNode(nodeId).get();
        node.setUID(placeholder);
      });
    });
  }

  public static void resetRequiredNames(Cluster cluster, String placeholder) {
    rangeClosed(1, cluster.getStripeCount()).forEach(stripeId -> {
      Stripe stripe = cluster.getStripe(stripeId).get();
      stripe.setName(placeholder);
      rangeClosed(1, stripe.getNodeCount()).forEach(nodeId -> {
        final Node node = stripe.getNode(nodeId).get();
        node.setName(placeholder);
      });
    });
  }

}
