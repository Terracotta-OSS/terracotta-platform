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
package org.terracotta.dynamic_config.cli.config_tool.command;

import org.terracotta.common.struct.TimeUnit;
import org.terracotta.diagnostic.client.connection.ConcurrencySizing;
import org.terracotta.diagnostic.client.connection.ConcurrentDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.cli.command.RemoteMainCommand;
import org.terracotta.dynamic_config.cli.config_tool.nomad.DefaultNomadManager;
import org.terracotta.dynamic_config.cli.config_tool.nomad.LockAwareNomadManager;
import org.terracotta.dynamic_config.cli.config_tool.nomad.NomadManager;
import org.terracotta.dynamic_config.cli.config_tool.restart.RestartService;
import org.terracotta.dynamic_config.cli.config_tool.stop.StopService;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.entity.client.NomadEntity;
import org.terracotta.nomad.entity.client.NomadEntityProvider;

import java.time.Duration;
import java.util.Collection;

import static java.util.Arrays.asList;

/**
 * @author Mathieu Carbou
 */
public class OssServiceProvider implements ServiceProvider {
  @Override
  public Collection<Object> createServices(RemoteMainCommand mainCommand) {
    return asList(
        createDiagnosticServiceProvider(mainCommand),
        createMultiDiagnosticServiceProvider(mainCommand),
        createNomadManager(mainCommand),
        createRestartService(mainCommand),
        createStopService(mainCommand),
        createObjectMapperFactory(mainCommand),
        createNomadEntityProvider(mainCommand));
  }

  protected StopService createStopService(RemoteMainCommand mainCommand) {
    return new StopService(createDiagnosticServiceProvider(mainCommand), getConcurrencySizing(mainCommand));
  }

  protected RestartService createRestartService(RemoteMainCommand mainCommand) {
    return new RestartService(createDiagnosticServiceProvider(mainCommand), getConcurrencySizing(mainCommand));
  }

  protected NomadManager<NodeContext> createNomadManager(RemoteMainCommand mainCommand) {
    NomadManager<NodeContext> nomadManager = new DefaultNomadManager<>(new NomadEnvironment(), createMultiDiagnosticServiceProvider(mainCommand), createNomadEntityProvider(mainCommand));
    if (mainCommand.getLockToken() != null) {
      nomadManager = new LockAwareNomadManager<>(mainCommand.getLockToken(), nomadManager);
    }
    return nomadManager;
  }

  protected NomadEntityProvider createNomadEntityProvider(RemoteMainCommand mainCommand) {
    return new NomadEntityProvider(
        "CONFIG-TOOL",
        getEntityConnectionTimeout(mainCommand),
        // A long timeout is important here.
        // We need to block the call and wait for any return.
        // We cannot timeout shortly otherwise we won't know the outcome of the 2PC Nomad transaction in case of a failover.
        new NomadEntity.Settings().setRequestTimeout(getEntityOperationTimeout(mainCommand)),
        mainCommand.getSecurityRootDirectory());
  }

  protected ConcurrentDiagnosticServiceProvider<UID> createMultiDiagnosticServiceProvider(RemoteMainCommand mainCommand) {
    return new ConcurrentDiagnosticServiceProvider<>(
        createDiagnosticServiceProvider(mainCommand),
        getConnectionTimeout(mainCommand),
        getConcurrencySizing(mainCommand));
  }

  protected DiagnosticServiceProvider createDiagnosticServiceProvider(RemoteMainCommand mainCommand) {
    return new DiagnosticServiceProvider("CONFIG-TOOL",
        getConnectionTimeout(mainCommand),
        getRequestTimeout(mainCommand),
        mainCommand.getSecurityRootDirectory(),
        createObjectMapperFactory(mainCommand));
  }

  protected ObjectMapperFactory createObjectMapperFactory(RemoteMainCommand mainCommand) {
    return new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule());
  }

  protected Duration getEntityConnectionTimeout(RemoteMainCommand mainCommand) {
    return Duration.ofMillis(mainCommand.getEntityConnectionTimeout().getQuantity(TimeUnit.MILLISECONDS));
  }

  protected Duration getEntityOperationTimeout(RemoteMainCommand mainCommand) {
    return Duration.ofMillis(mainCommand.getEntityOperationTimeout().getQuantity(TimeUnit.MILLISECONDS));
  }

  protected Duration getRequestTimeout(RemoteMainCommand mainCommand) {
    return Duration.ofMillis(mainCommand.getRequestTimeout().getQuantity(TimeUnit.MILLISECONDS));
  }

  protected Duration getConnectionTimeout(RemoteMainCommand mainCommand) {
    return Duration.ofMillis(mainCommand.getConnectionTimeout().getQuantity(TimeUnit.MILLISECONDS));
  }

  protected ConcurrencySizing getConcurrencySizing(RemoteMainCommand mainCommand) {
    return new ConcurrencySizing();
  }

}
