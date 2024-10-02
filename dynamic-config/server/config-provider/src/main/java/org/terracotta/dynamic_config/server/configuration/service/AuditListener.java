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
package org.terracotta.dynamic_config.server.configuration.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.server.DynamicConfigListener;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeState;
import org.terracotta.server.Server;

import java.util.Properties;

public class AuditListener implements DynamicConfigListener {
  private final Server server;

  public AuditListener(Server server) {this.server = server;}

  @Override
  public void onSettingChanged(SettingNomadChange change, Cluster updated) {
    server.audit(change.getSummary() + " invoked", new Properties());
  }

  @Override
  public void onNodeRemoval(UID stripeUID, Node removedNode) {
    server.audit("Detach invoked for node " + removedNode.getName(), new Properties());
  }

  @Override
  public void onNodeAddition(UID stripeUID, Node addedNode) {
    server.audit("Attach invoked for node " + addedNode.getName(), new Properties());
  }

  @Override
  public void onStripeAddition(Stripe addedStripe) {
    server.audit("Attach invoked for stripe " + addedStripe.getName(), new Properties());
  }

  @Override
  public void onStripeRemoval(Stripe removedStripe) {
    server.audit("Detach invoked for stripe " + removedStripe.getName(), new Properties());
  }

  @Override
  public void onNomadCommit(CommitMessage message, AcceptRejectResponse response, ChangeState<NodeContext> changeState) {
    server.audit("Nomad change " + message.getChangeUuid() + " committed", new Properties());
  }

  @Override
  public void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {
    server.audit("Nomad change " + message.getChangeUuid() + " rolled back", new Properties());
  }
}
