/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.dynamic_config.test_support.provider;

import com.tc.classloader.BuiltinService;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.api.server.DelegatingDynamicConfigNomadServer;
import org.terracotta.dynamic_config.api.server.DynamicConfigNomadServer;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadException;

import java.util.Collection;
import java.util.Collections;

@BuiltinService
public class ReplicaSimulationServiceProvider implements ServiceProvider {
  public static final String SIMULATE_ACTION_TC_PROP = "org.terracotta.ReplicaSimulationHandler.action";
  private volatile boolean commitFailureFired;
  private volatile boolean prepareFailureFired;
  private volatile boolean rollbackFailureFired;

  @Override
  public boolean initialize(ServiceProviderConfiguration config, PlatformConfiguration platformConfig) {
    TopologyService topologyService = platformConfig.getExtendedConfiguration(TopologyService.class).iterator().next();
    DynamicConfigNomadServer nomadServer = platformConfig.getExtendedConfiguration(DynamicConfigNomadServer.class).iterator().next();
    DynamicConfigNomadServer realDelegate = ((DelegatingDynamicConfigNomadServer) nomadServer).getDelegate();
    DelegatingDynamicConfigNomadServer testDelegate = new DelegatingDynamicConfigNomadServer(realDelegate) {
      @Override
      public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
        if ("prepare-failure".equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(SIMULATE_ACTION_TC_PROP))) {
          if (!prepareFailureFired) {
            prepareFailureFired = true;
            throw new IllegalArgumentException("prepare failed due to test simulation");
          }
        }
        return realDelegate.prepare(message);
      }

      @Override
      public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
        if ("commit-failure".equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(SIMULATE_ACTION_TC_PROP))) {
          if (!commitFailureFired) {
            commitFailureFired = true;
            throw new IllegalArgumentException("commit failed due to test simulation");
          }
        }
        return realDelegate.commit(message);
      }

      @Override
      public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
        if ("rollback-failure".equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(SIMULATE_ACTION_TC_PROP))) {
          if (!rollbackFailureFired) {
            rollbackFailureFired = true;
            throw new IllegalArgumentException("commit failed due to test simulation");
          }
        }
        return realDelegate.rollback(message);
      }
    };
    ((DelegatingDynamicConfigNomadServer) nomadServer).setDelegate(testDelegate);
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    return null;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.emptyList();
  }

  @Override
  public void prepareForSynchronization() {
  }
}
