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
package org.terracotta.dynamic_config.xml;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.ObjectFactory;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcCluster;
import org.w3c.dom.Element;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClusterConfiguration {
  // please keep an ordering
  private final Map<Integer, StripeConfiguration> stripeIdConfigInfo = new LinkedHashMap<>();
  private final String clusterName;
  private final int stripeId;

  public ClusterConfiguration(Cluster cluster, int stripeId, PathResolver pathResolver) {
    this.stripeId = stripeId;
    List<Stripe> stripes = cluster.getStripes();
    for (int i = 0; i < stripes.size(); i++) {
      stripeIdConfigInfo.put(i + 1, newStripe(pathResolver, stripes.get(i)));
    }
    this.clusterName = cluster.getName();
  }

  public StripeConfiguration get(int stripeId) {
    return stripeIdConfigInfo.get(stripeId);
  }

  protected StripeConfiguration newStripe(PathResolver pathResolver, Stripe stripe) {
    return new StripeConfiguration(stripe, pathResolver);
  }

  public Element getClusterElement() {
    ObjectFactory factory = new ObjectFactory();

    TcCluster cluster = factory.createTcCluster();
    cluster.setName(clusterName);
    cluster.setCurrentStripeId(stripeId);

    for (Map.Entry<Integer, StripeConfiguration> entry : stripeIdConfigInfo.entrySet()) {
      cluster.getStripes().add(entry.getValue().getClusterConfigStripe(factory));
    }

    return Utils.createElement(factory.createCluster(cluster));
  }

  @Override
  public String toString() {
    return "ClusterConfiguration{" +
        "stripeIdConfigInfo=" + stripeIdConfigInfo +
        ", clusterName='" + clusterName + '\'' +
        '}';
  }
}
