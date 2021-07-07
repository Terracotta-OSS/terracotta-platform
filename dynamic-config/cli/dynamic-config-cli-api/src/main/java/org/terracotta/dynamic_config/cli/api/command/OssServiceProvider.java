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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.common.struct.TimeUnit;
import org.terracotta.diagnostic.client.connection.ConcurrencySizing;
import org.terracotta.diagnostic.client.connection.ConcurrentDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DefaultDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.cli.api.nomad.DefaultNomadManager;
import org.terracotta.dynamic_config.cli.api.nomad.LockAwareNomadManager;
import org.terracotta.dynamic_config.cli.api.nomad.NomadManager;
import org.terracotta.dynamic_config.cli.api.output.OutputService;
import org.terracotta.dynamic_config.cli.api.restart.RestartService;
import org.terracotta.dynamic_config.cli.api.stop.StopService;
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
  public Collection<Object> createServices(Configuration config) {
    return asList(
        createDiagnosticServiceProvider(config),
        createMultiDiagnosticServiceProvider(config),
        createNomadManager(config),
        createRestartService(config),
        createStopService(config),
        createObjectMapperFactory(config),
        createNomadEntityProvider(config),
        createOutputService(config));
  }

  protected OutputService createOutputService(Configuration config) {
    // we could enhance the output service if we want
    return config.getOutputService();
  }

  protected StopService createStopService(Configuration config) {
    return new StopService(createDiagnosticServiceProvider(config), getConcurrencySizing(config));
  }

  protected RestartService createRestartService(Configuration config) {
    return new RestartService(createDiagnosticServiceProvider(config), getConcurrencySizing(config));
  }

  protected NomadManager<NodeContext> createNomadManager(Configuration config) {
    NomadManager<NodeContext> nomadManager = new DefaultNomadManager<>(new NomadEnvironment(), createMultiDiagnosticServiceProvider(config), createNomadEntityProvider(config));
    if (config.getLockToken() != null) {
      nomadManager = new LockAwareNomadManager<>(config.getLockToken(), nomadManager);
    }
    return nomadManager;
  }

  protected NomadEntityProvider createNomadEntityProvider(Configuration config) {
    return new NomadEntityProvider(
        "CONFIG-TOOL",
        getConnectionTimeout(config),
        // A long timeout is important here.
        // We need to block the call and wait for any return.
        // We cannot timeout shortly otherwise we won't know the outcome of the 2PC Nomad transaction in case of a failover.
        new NomadEntity.Settings().setRequestTimeout(getEntityOperationTimeout(config)),
        config.getSecurityRootDirectory());
  }

  protected ConcurrentDiagnosticServiceProvider<UID> createMultiDiagnosticServiceProvider(Configuration config) {
    return new ConcurrentDiagnosticServiceProvider<>(
        createDiagnosticServiceProvider(config),
        getConnectionTimeout(config),
        getConcurrencySizing(config));
  }

  protected DiagnosticServiceProvider createDiagnosticServiceProvider(Configuration config) {
    return new DefaultDiagnosticServiceProvider("CONFIG-TOOL",
        getConnectionTimeout(config),
        getRequestTimeout(config),
        config.getSecurityRootDirectory(),
        createObjectMapperFactory(config));
  }

  protected ObjectMapperFactory createObjectMapperFactory(Configuration config) {
    return new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule());
  }

  protected Duration getEntityOperationTimeout(Configuration config) {
    return Duration.ofMillis(config.getEntityOperationTimeout().getQuantity(TimeUnit.MILLISECONDS));
  }

  protected Duration getRequestTimeout(Configuration config) {
    return Duration.ofMillis(config.getRequestTimeout().getQuantity(TimeUnit.MILLISECONDS));
  }

  protected Duration getConnectionTimeout(Configuration config) {
    return Duration.ofMillis(config.getConnectionTimeout().getQuantity(TimeUnit.MILLISECONDS));
  }

  protected ConcurrencySizing getConcurrencySizing(Configuration config) {
    return new ConcurrencySizing();
  }

}
