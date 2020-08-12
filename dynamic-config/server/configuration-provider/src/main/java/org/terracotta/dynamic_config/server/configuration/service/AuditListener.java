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
package org.terracotta.dynamic_config.server.configuration.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.server.api.DynamicConfigListener;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;

import java.util.Properties;
import java.util.stream.Collectors;

public class AuditListener implements DynamicConfigListener {
  private final Server server = ServerEnv.getServer();

  @Override
  public void onSettingChanged(SettingNomadChange change, Cluster updated) {
    server.audit(change.getSummary() + " invoked", new Properties());
  }

  @Override
  public void onNodeRemoval(int stripeId, Node removedNode) {
    server.audit("Detach invoked for node " + removedNode.getName(), new Properties());
  }

  @Override
  public void onNodeAddition(int stripeId, Node addedNode) {
    server.audit("Attach invoked for node " + addedNode.getName(), new Properties());
  }

  @Override
  public void onStripeAddition(Stripe addedStripe) {
    server.audit("Attach invoked for stripe with nodes " + getNodeNames(addedStripe), new Properties());
  }

  @Override
  public void onStripeRemoval(Stripe removedStripe) {
    server.audit("Detach invoked for stripe with nodes " + getNodeNames(removedStripe), new Properties());
  }

  @Override
  public void onNomadCommit(CommitMessage message, AcceptRejectResponse response, NomadChangeInfo changeInfo) {
    server.audit("Nomad change " + message.getChangeUuid() + " committed", new Properties());
  }

  @Override
  public void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {
    server.audit("Nomad change " + message.getChangeUuid() + " rolled back", new Properties());
  }

  private String getNodeNames(Stripe addedStripe) {
    return addedStripe.getNodes().stream().map(Node::getName).collect(Collectors.joining(", "));
  }
}
