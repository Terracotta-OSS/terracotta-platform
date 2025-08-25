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
package org.terracotta.dynamic_config.server.configuration.startup;

import com.tc.text.PrettyPrintable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.FailoverBehavior;
import org.terracotta.configuration.ServerConfiguration;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.server.api.DynamicConfigExtension;
import org.terracotta.dynamic_config.server.api.GroupPortMapper;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.json.Json;
import org.terracotta.server.Server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.terracotta.common.struct.Tuple2.tuple2;

public class StartupConfiguration implements Configuration, PrettyPrintable, StateDumpable, PlatformConfiguration, DynamicConfigExtension.Registrar {

  private static final Logger LOGGER = LoggerFactory.getLogger(StartupConfiguration.class);
  
  private final Collection<Tuple2<Class<?>, Supplier<?>>> extendedConfigurations = new CopyOnWriteArrayList<>();
  private final Collection<ServiceProviderConfiguration> serviceProviderConfigurations = new CopyOnWriteArrayList<>();

  private final Supplier<NodeContext> nodeContextSupplier;
  private final boolean unConfigured;
  private final boolean repairMode;
  private final ClassLoader classLoader;
  private final PathResolver pathResolver;
  private final IParameterSubstitutor substitutor;
  private final Json json;
  private final GroupPortMapper groupPortMapper;

  StartupConfiguration(Supplier<NodeContext> nodeContextSupplier, boolean unConfigured, boolean repairMode, ClassLoader classLoader, PathResolver pathResolver, IParameterSubstitutor substitutor, Json.Factory jsonFactory, Server server) {
    this.nodeContextSupplier = requireNonNull(nodeContextSupplier);
    this.unConfigured = unConfigured;
    this.repairMode = repairMode;
    this.classLoader = requireNonNull(classLoader);
    this.pathResolver = requireNonNull(pathResolver);
    this.substitutor = requireNonNull(substitutor);
    this.json = jsonFactory.create();
    Collection<Class<? extends GroupPortMapper>> mappers = server.getImplementations(GroupPortMapper.class);
    Class<? extends GroupPortMapper> gi = mappers.iterator().next();
    GroupPortMapper mapper;
    try {
      mapper = gi.newInstance();
    } catch (IllegalAccessException | InstantiationException i) {
      mapper = new DefaultGroupPortMapperImpl();
    }
    this.groupPortMapper = mapper;
  }

  @Override
  public <T> List<T> getExtendedConfiguration(Class<T> type) {
    requireNonNull(type);
    List<T> out = new ArrayList<>(1);
    for (Tuple2<Class<?>, Supplier<?>> extendedConfiguration : extendedConfigurations) {
      if (extendedConfiguration.t1 == type) {
        Object o = extendedConfiguration.t2.get();
        if (o != null) {
          out.add(type.cast(o));
        }
      } else if (extendedConfiguration.t1 == null) {
        Object o = extendedConfiguration.t2.get();
        if (type.isInstance(o)) {
          out.add(type.cast(o));
        }
      } else if (extendedConfiguration.t1.getName().equals(type.getName())) {
        throw new IllegalArgumentException("Requested service type " + type + " from classloader " + type.getClassLoader() + " but has service " + extendedConfiguration.t1 + " from classlaoder " + extendedConfiguration.t1.getClassLoader());
      }
    }
    return out;
  }

  @Override
  public List<ServiceProviderConfiguration> getServiceConfigurations() {
    return new ArrayList<>(serviceProviderConfigurations);
  }

  @Override
  public boolean isPartialConfiguration() {
    return unConfigured || repairMode;
  }

  @Override
  public ServerConfiguration getServerConfiguration() {
    return toServerConfiguration(nodeContextSupplier.get().getNode());
  }

  @Override
  public List<ServerConfiguration> getServerConfigurations() {
    return nodeContextSupplier.get().getStripe().getNodes().stream().map(this::toServerConfiguration).collect(toList());
  }

  @Override
  public String getRawConfiguration() {
    return Props.toString(nodeContextSupplier.get().getCluster().toProperties(false, false, false));
  }

  /**
   * This is shown in the logs. We need to show the whole effective config that will be in place, resolved
   */
  @Override
  public String toString() {
    return getRawConfiguration();
  }

  @Override
  public Properties getTcProperties() {
    Properties copy = new Properties();
    copy.putAll(nodeContextSupplier.get().getNode().getTcProperties().orDefault());
    return copy;
  }

  @Override
  public FailoverBehavior getFailoverPriority() {
    final FailoverPriority failoverPriority = nodeContextSupplier.get().getCluster().getFailoverPriority().orElse(null);
    if (failoverPriority == null) {
      return null;
    }
    switch (failoverPriority.getType()) {
      case CONSISTENCY:
        return new FailoverBehavior(FailoverBehavior.Type.CONSISTENCY, failoverPriority.getVoters());
      case AVAILABILITY:
        return new FailoverBehavior(FailoverBehavior.Type.AVAILABILITY, 0);
      default:
        throw new AssertionError(failoverPriority.getType());
    }
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> main = new LinkedHashMap<>();
    StateDumpCollector collector = createCollector("collector", main);

    StateDumpCollector startupConfig = collector.subStateDumpCollector(getClass().getName());
    startupConfig.addState("unConfigured", unConfigured);
    startupConfig.addState("repairMode", repairMode);
    startupConfig.addState("partialConfig", isPartialConfiguration());
    startupConfig.addState("startupNodeContext", toMap(nodeContextSupplier.get()));

    StateDumpCollector platformConfig = collector.subStateDumpCollector(PlatformConfiguration.class.getName());
    addStateTo(platformConfig);

    StateDumpCollector serviceProviderConfigurations = collector.subStateDumpCollector("ServiceProviderConfigurations");
    this.serviceProviderConfigurations.stream()
        .filter(StateDumpable.class::isInstance)
        .map(StateDumpable.class::cast)
        .forEach(sd -> sd.addStateTo(serviceProviderConfigurations.subStateDumpCollector(sd.getClass().getName())));

    return main;
  }

  private Map<String, ?> toMap(Object o) {
    return json.mapToObject(o);
  }

  private StateDumpCollector createCollector(String name, Map<String, Object> map) {
    return new StateDumpCollector() {
      @Override
      public StateDumpCollector subStateDumpCollector(String name) {
        Map<String, Object> sub = new LinkedHashMap<>();
        map.put(name, sub);
        return createCollector(name, sub);
      }

      @Override
      public void addState(String key, Object value) {
        map.put(key, value);
      }
    };
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("serverName", getServerName());
    stateDumpCollector.addState("host", getHost());
    stateDumpCollector.addState("tsaPort", getTsaPort());
    StateDumpCollector extendedConfigurations = stateDumpCollector.subStateDumpCollector("ExtendedConfigs");
    this.extendedConfigurations.stream()
        .map(tuple -> tuple.getT2().get())
        .filter(StateDumpable.class::isInstance)
        .map(StateDumpable.class::cast)
        .forEach(sd -> sd.addStateTo(extendedConfigurations.subStateDumpCollector(sd.getClass().getName())));
  }

  @Override
  public String getServerName() {
    return nodeContextSupplier.get().getNode().getName();
  }

  @Override
  public String getHost() {
    return nodeContextSupplier.get().getNode().getHostname();
  }

  @Override
  public int getTsaPort() {
    return nodeContextSupplier.get().getNode().getPort().orDefault();
  }

  @Override
  public void registerExtendedConfiguration(Object o) {
    registerExtendedConfiguration(null, o);
  }

  @Override
  public <T> void registerExtendedConfigurationSupplier(Class<T> type, Supplier<T> supplier) {
    extendedConfigurations.add(tuple2(type, supplier));
  }

  @Override
  public void registerServiceProviderConfiguration(ServiceProviderConfiguration serviceProviderConfiguration) {
    serviceProviderConfigurations.add(serviceProviderConfiguration);
  }
  
  public void close() {
    extendedConfigurations.forEach(action->{
      Object o = action.getT2().get();
      if (o instanceof AutoCloseable) {
        try {
          ((AutoCloseable) o).close();
        } catch (Exception e) {
          LOGGER.info("failed to close extended configuration of type {}", action.getT1(), e);
        }
      }
    });
  }

  public void discoverExtensions() {
    boolean configuredNode = !isPartialConfiguration();
    for (DynamicConfigExtension dynamicConfigExtension : ServiceLoader.load(DynamicConfigExtension.class, classLoader)) {
      if (configuredNode || !dynamicConfigExtension.onlyWhenNodeConfigured()) {
        dynamicConfigExtension.configure(this, this);
      }
    }
  }

  private ServerConfiguration toServerConfiguration(Node node) {
    return new DynamicConfigServerConfiguration(node, nodeContextSupplier, substitutor, groupPortMapper, pathResolver, unConfigured);
  }
}
