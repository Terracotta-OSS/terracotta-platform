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
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;

public interface Applicability {

  static Applicability cluster() {
    return new DefaultApplicability(CLUSTER, null, null);
  }

  static Applicability stripe(UID stripeUID) {
    return new DefaultApplicability(STRIPE, requireNonNull(stripeUID), null);
  }

  static Applicability node(UID nodeUID) {
    return new DefaultApplicability(NODE, null, requireNonNull(nodeUID));
  }

  Scope getLevel();

  Optional<Stripe> getStripe(Cluster cluster);

  Optional<Node> getNode(Cluster cluster);

  boolean isApplicableTo(NodeContext node);
}
