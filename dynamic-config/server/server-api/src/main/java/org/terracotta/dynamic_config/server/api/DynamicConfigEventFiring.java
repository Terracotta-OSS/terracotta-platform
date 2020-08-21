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
package org.terracotta.dynamic_config.server.api;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadChangeInfo;

/**
 * @author Mathieu Carbou
 */
public interface DynamicConfigEventFiring {

  void onNewConfigurationSaved(NodeContext nodeContext, Long version);

  void onSettingChanged(SettingNomadChange change, Cluster updated);

  void onNodeRemoval(UID stripeUID, Node removedNode);

  void onNodeAddition(UID stripeUID, Node addedNode);

  void onNomadPrepare(PrepareMessage message, AcceptRejectResponse response);

  void onNomadCommit(CommitMessage message, AcceptRejectResponse response, NomadChangeInfo changeInfo);

  void onNomadRollback(RollbackMessage message, AcceptRejectResponse response);

  void onStripeAddition(Stripe addedStripe);

  void onStripeRemoval(Stripe removedStripe);
}
