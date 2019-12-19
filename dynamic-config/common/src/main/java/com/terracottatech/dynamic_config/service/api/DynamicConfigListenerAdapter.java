/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.api;

import com.tc.classloader.CommonComponent;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RollbackMessage;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public class DynamicConfigListenerAdapter implements DynamicConfigListener {

  private final Supplier<DynamicConfigListener> supplier;

  public DynamicConfigListenerAdapter(Supplier<DynamicConfigListener> supplier) {
    this.supplier = supplier;
  }

  @Override
  public void onNewConfigurationAppliedAtRuntime(NodeContext nodeContext, Configuration configuration) {
    getDelegate().ifPresent(listener -> listener.onNewConfigurationAppliedAtRuntime(nodeContext, configuration));
  }

  @Override
  public void onNewConfigurationPendingRestart(NodeContext nodeContext, Configuration configuration) {
    getDelegate().ifPresent(listener -> listener.onNewConfigurationPendingRestart(nodeContext, configuration));
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
  public void onNomadCommit(CommitMessage message, AcceptRejectResponse response) {
    getDelegate().ifPresent(listener -> listener.onNomadCommit(message, response));
  }

  @Override
  public void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {
    getDelegate().ifPresent(listener -> listener.onNomadRollback(message, response));
  }

  private Optional<DynamicConfigListener> getDelegate() {
    return Optional.ofNullable(supplier.get());
  }
}
