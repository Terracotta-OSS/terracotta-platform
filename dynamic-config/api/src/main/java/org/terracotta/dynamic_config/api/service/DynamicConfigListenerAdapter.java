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
package org.terracotta.dynamic_config.api.service;

import com.tc.classloader.CommonComponent;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadChangeInfo;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public class DynamicConfigListenerAdapter implements DynamicConfigListener {

  private final Supplier<DynamicConfigListener> supplier;

  public DynamicConfigListenerAdapter() {
    this(() -> null);
  }

  public DynamicConfigListenerAdapter(Supplier<DynamicConfigListener> supplier) {
    this.supplier = supplier;
  }

  @Override
  public void onSettingChanged(SettingNomadChange change, Cluster updated) {
    getDelegate().ifPresent(listener -> listener.onSettingChanged(change, updated));
  }

  @Override
  public void onNewConfigurationSaved(NodeContext nodeContext, Long version) {
    getDelegate().ifPresent(listener -> listener.onNewConfigurationSaved(nodeContext, version));
  }

  @Override
  public void onNomadPrepare(PrepareMessage message, AcceptRejectResponse response) {
    getDelegate().ifPresent(listener -> listener.onNomadPrepare(message, response));
  }

  @Override
  public void onNomadCommit(CommitMessage message, AcceptRejectResponse response, NomadChangeInfo changeInfo) {
    getDelegate().ifPresent(listener -> listener.onNomadCommit(message, response, changeInfo));
  }

  @Override
  public void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {
    getDelegate().ifPresent(listener -> listener.onNomadRollback(message, response));
  }

  @Override
  public void onNodeRemoval(int stripeId, Node removedNode) {
    getDelegate().ifPresent(listener -> listener.onNodeRemoval(stripeId, removedNode));
  }

  @Override
  public void onNodeAddition(int stripeId, Node addedNode) {
    getDelegate().ifPresent(listener -> listener.onNodeAddition(stripeId, addedNode));
  }

  private Optional<DynamicConfigListener> getDelegate() {
    return Optional.ofNullable(supplier.get());
  }
}
