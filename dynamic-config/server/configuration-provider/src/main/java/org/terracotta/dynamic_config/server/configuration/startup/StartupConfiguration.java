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
package org.terracotta.dynamic_config.server.configuration.startup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tc.services.MappedStateCollector;
import com.tc.text.PrettyPrintable;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.FailoverBehavior;
import org.terracotta.configuration.ServerConfiguration;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.server.api.DynamicConfigExtension;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.json.Json;
import org.terracotta.monitoring.PlatformService;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.terracotta.common.struct.Tuple2.tuple2;

public class StartupConfiguration implements Configuration, PrettyPrintable, StateDumpable, PlatformConfiguration, DynamicConfigExtension.Registrar {

  private final Collection<Tuple2<Class<?>, Object>> extendedConfigurations = new CopyOnWriteArrayList<>();
  private final Collection<ServiceProviderConfiguration> serviceProviderConfigurations = new CopyOnWriteArrayList<>();

  private final NodeContext nodeContext;
  private final boolean unConfigured;
  private final boolean repairMode;
  private final ClassLoader classLoader;
  private final PathResolver pathResolver;
  private final IParameterSubstitutor substitutor;

  StartupConfiguration(NodeContext nodeContext, boolean unConfigured, boolean repairMode, ClassLoader classLoader, PathResolver pathResolver, IParameterSubstitutor substitutor) {
    this.nodeContext = requireNonNull(nodeContext);
    this.unConfigured = unConfigured;
    this.repairMode = repairMode;
    this.classLoader = requireNonNull(classLoader);
    this.pathResolver = pathResolver;
    this.substitutor = substitutor;
  }

  @Override
  public <T> List<T> getExtendedConfiguration(Class<T> type) {
    requireNonNull(type);
    List<T> out = new ArrayList<>(1);
    for (Tuple2<Class<?>, Object> extendedConfiguration : extendedConfigurations) {
      if (extendedConfiguration.t1 == type) {
        out.add(type.cast(extendedConfiguration.t2));
      } else if (extendedConfiguration.t1 == null && type.isInstance(extendedConfiguration.t2)) {
        out.add(type.cast(extendedConfiguration.t2));
      } else if (extendedConfiguration.t1 != null && extendedConfiguration.t1.getName().equals(type.getName())) {
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
    return toServerConfiguration(nodeContext.getNode());
  }

  @Override
  public List<ServerConfiguration> getServerConfigurations() {
    return nodeContext.getStripe().getNodes().stream().map(this::toServerConfiguration).collect(toList());
  }

  /**
   * Consumed by {@link PlatformService#getPlatformConfiguration()} to output the configuration.
   * <p>
   * MnM is using that and it needs to have a better view that what we had before (props resolved and also user set added in a separated section)
   * <p>
   * So this mimics what the export command would do
   */
  @Override
  public String getRawConfiguration() {
    Cluster cluster = nodeContext.getCluster();
    Properties nonDefaults = cluster.toProperties(false, false);
    substitute(nonDefaults);
    try (StringWriter out = new StringWriter()) {
      Props.store(out, nonDefaults, "User-defined configurations for node '" + nodeContext.getNodeName() + "' in stripe ID " + nodeContext.getStripeId());
      return out.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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
    copy.putAll(nodeContext.getNode().getTcProperties());
    return copy;
  }

  @Override
  public FailoverBehavior getFailoverPriority() {
    final FailoverPriority failoverPriority = nodeContext.getNode().getFailoverPriority();
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
    MappedStateCollector collector = new MappedStateCollector("collector");

    StateDumpCollector startupConfig = collector.subStateDumpCollector(getClass().getName());
    startupConfig.addState("unConfigured", unConfigured);
    startupConfig.addState("repairMode", repairMode);
    startupConfig.addState("partialConfig", isPartialConfiguration());
    startupConfig.addState("startupNodeContext", Json.parse(Json.toJson(nodeContext), new TypeReference<Map<String, ?>>() {}));

    StateDumpCollector platformConfig = collector.subStateDumpCollector(PlatformConfiguration.class.getName());
    addStateTo(platformConfig);

    StateDumpCollector serviceProviderConfigurations = collector.subStateDumpCollector("ServiceProviderConfigurations");
    this.serviceProviderConfigurations.stream()
        .filter(StateDumpable.class::isInstance)
        .map(StateDumpable.class::cast)
        .forEach(sd -> sd.addStateTo(serviceProviderConfigurations.subStateDumpCollector(sd.getClass().getName())));

    return collector.getMap();
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("serverName", getServerName());
    stateDumpCollector.addState("host", getHost());
    stateDumpCollector.addState("tsaPort", getTsaPort());
    StateDumpCollector extendedConfigurations = stateDumpCollector.subStateDumpCollector("ExtendedConfigs");
    this.extendedConfigurations.stream()
        .map(Tuple2::getT2)
        .filter(StateDumpable.class::isInstance)
        .map(StateDumpable.class::cast)
        .forEach(sd -> sd.addStateTo(extendedConfigurations.subStateDumpCollector(sd.getClass().getName())));
  }

  @Override
  public String getServerName() {
    return nodeContext.getNodeName();
  }

  @Override
  public String getHost() {
    return nodeContext.getNode().getNodeHostname();
  }

  @Override
  public int getTsaPort() {
    return nodeContext.getNode().getNodePort();
  }

  @Override
  public void registerExtendedConfiguration(Object o) {
    registerExtendedConfiguration(null, o);
  }

  @Override
  public <T> void registerExtendedConfiguration(Class<T> type, T implementation) {
    extendedConfigurations.add(tuple2(type, implementation));
  }

  @Override
  public void registerServiceProviderConfiguration(ServiceProviderConfiguration serviceProviderConfiguration) {
    serviceProviderConfigurations.add(serviceProviderConfiguration);
  }

  public void discoverExtensions() {
    for (DynamicConfigExtension dynamicConfigExtension : ServiceLoader.load(DynamicConfigExtension.class, classLoader)) {
      dynamicConfigExtension.configure(this, this);
    }
  }

  private void substitute(Properties properties) {
    int stripeId = nodeContext.getStripeId();
    int nodeId = nodeContext.getNodeId();
    String prefix = "stripe." + stripeId + ".node." + nodeId + ".";
    properties.stringPropertyNames().stream()
        .filter(key -> !key.startsWith("stripe.") || key.startsWith(prefix)) // we only substitute cluster-wide parameters plus this node's parameters
        .forEach(key -> properties.setProperty(key, substitutor.substitute(properties.getProperty(key))));
  }

  private ServerConfiguration toServerConfiguration(Node node) {
    return new ServerConfiguration() {
      @Override
      public InetSocketAddress getTsaPort() {
        return InetSocketAddress.createUnresolved(substitutor.substitute(node.getNodeBindAddress()), node.getNodePort());
      }

      @Override
      public InetSocketAddress getGroupPort() {
        return InetSocketAddress.createUnresolved(substitutor.substitute(node.getNodeGroupBindAddress()), node.getNodeGroupPort());
      }

      @Override
      public String getHost() {
        // substitutions not allowed on hostname since hostname-port is a key to identify a node
        // any substitution is allowed but resolved eagerly when parsing the CLI
        return node.getNodeHostname();
      }

      @Override
      public String getName() {
        // substitutions not allowed on name since stripe ID / name is a key to identify a node
        return node.getNodeName();
      }

      @Override
      public int getClientReconnectWindow() {
        return node.getClientReconnectWindow().getExactQuantity(TimeUnit.SECONDS).intValueExact();
      }

      @Override
      public void setClientReconnectWindow(int value) {
        node.setClientReconnectWindow(value, TimeUnit.SECONDS);
      }

      @Override
      public File getLogsLocation() {
        return substitutor.substitute(pathResolver.resolve(node.getNodeLogDir())).toFile();
      }

      @Override
      public String toString() {
        return node.getNodeName() + "@" + node.getNodeAddress();
      }
    };
  }
}
