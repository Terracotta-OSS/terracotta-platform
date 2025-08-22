/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventService;
import org.terracotta.dynamic_config.server.api.DynamicConfigListener;
import org.terracotta.dynamic_config.server.api.EventRegistration;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigEventServiceImpl implements DynamicConfigEventService, DynamicConfigEventFiring {

  private final List<DynamicConfigListener> listeners = new CopyOnWriteArrayList<>();

  @Override
  public EventRegistration register(DynamicConfigListener listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  @Override
  public void onSettingChanged(SettingNomadChange change, Cluster updated) {
    listeners.forEach(c -> c.onSettingChanged(change, updated));
  }

  @Override
  public void onNewConfigurationSaved(NodeContext nodeContext, Long version) {
    listeners.forEach(c -> c.onNewConfigurationSaved(nodeContext, version));
  }

  @Override
  public void onNodeRemoval(UID stripeUID, Node removedNode) {
    listeners.forEach(c -> c.onNodeRemoval(stripeUID, removedNode));
  }

  @Override
  public void onNodeAddition(UID stripeUID, Node addedNode) {
    listeners.forEach(c -> c.onNodeAddition(stripeUID, addedNode));
  }

  @Override
  public void onStripeAddition(Stripe addedStripe) {
    listeners.forEach(c -> c.onStripeAddition(addedStripe));
  }

  @Override
  public void onStripeRemoval(Stripe removedStripe) {
    listeners.forEach(c -> c.onStripeRemoval(removedStripe));
  }

  @Override
  public void onNomadPrepare(PrepareMessage message, AcceptRejectResponse response) {
    listeners.forEach(c -> c.onNomadPrepare(message, response));
  }

  @Override
  public void onNomadCommit(CommitMessage message, AcceptRejectResponse response, ChangeState<NodeContext> changeState) {
    listeners.forEach(c -> c.onNomadCommit(message, response, changeState));
  }

  @Override
  public void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {
    listeners.forEach(c -> c.onNomadRollback(message, response));
  }

}
