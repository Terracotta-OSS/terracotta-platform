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
package org.terracotta.platform.metrics;

import com.tc.classloader.BuiltinService;
import com.tc.spi.Guardian;
import com.tc.text.PrettyPrintable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.server.DynamicConfigEventService;
import org.terracotta.dynamic_config.api.server.DynamicConfigListener;
import org.terracotta.dynamic_config.api.server.EventRegistration;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.event.EventService;
import org.terracotta.platform.metrics.metric.MetricService;
import org.terracotta.platform.metrics.log.MetricLogger;
import org.terracotta.platform.metrics.log.MetricLoggingTrigger;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformService;
import org.terracotta.platform.metrics.platform.DefaultPlatformMetricService;
import org.terracotta.platform.metrics.voltron.MonitoringMetricCollector;
import org.terracotta.server.Server;

/**
 * Core service provider that provides an implementation of core's {@link MetricService} (gauges) and
 * {@link EventService} (events) in order to collect metrics.
 * It will also spawn a single thread to schedule the logging at periodic interval.
 *
 * <p>The {@link EventService} handed to the core intercepts {@code MEMBER_STATE}
 * (state of this server) and {@code PEER_STATE} (state of a peer, fired on the active) to trigger an
 * immediate log line when the stripe states change.
 */
@BuiltinService
public class MetricServiceProvider implements ServiceProvider, StateDumpable, PrettyPrintable, Closeable {
  private static final String METRIC_LOGGER_PERIOD_PROPERTY = "org.terracotta.platform.metrics.logging.period";
  private static final String METRIC_LOGGER_PERIOD_DEFAULT = "300";

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricServiceProvider.class);

  private MonitoringMetricCollector platformMetricService;
  private UID thisNodeUID;
  private MetricLogger metricLogger;
  private EventRegistration eventListener;


  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    Server server = getService(platformConfiguration, Server.class);
    TopologyService topologyService = getService(platformConfiguration, TopologyService.class);
    DynamicConfigEventService dynamicConfigEventService = getService(platformConfiguration, DynamicConfigEventService.class);

    // grab current node info
    thisNodeUID = topologyService.getRuntimeNodeContext().getNodeUID();

    // our own metric facade (as we understand them)
    platformMetricService = new MonitoringMetricCollector(server, (sendingServer, state) -> {
      metricLogger.logNow(MetricLoggingTrigger.TOPOLOGY_CHANGE);
    });
    DefaultPlatformMetricService service = new DefaultPlatformMetricService(server, platformMetricService);

    // periodic logger
    metricLogger = new MetricLogger(()->{
      platformMetricService.snapshot();
      return service;
    });

    // listen for DC changes that could update the setting
    eventListener = dynamicConfigEventService.register(new DynamicConfigListener() {
      @Override
      public void onSettingChanged(SettingNomadChange change, Cluster updated) {
        if (change.getSetting() == Setting.TC_PROPERTIES) {
          updated.getNode(thisNodeUID).ifPresent(MetricServiceProvider.this::updateLoggingPeriod);
        }
      }
    });

    // start metric logger
    updateLoggingPeriod(topologyService.getRuntimeNodeContext().getNode());
    metricLogger.start();

    return true;
  }

  @Override
  public void close() {
    eventListener.unregister();
    metricLogger.stop();
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> serviceConfiguration) {
    if (serviceConfiguration.getServiceType() == IStripeMonitoring.class) {
      return serviceConfiguration.getServiceType().cast(platformMetricService);
    }

    if (serviceConfiguration.getServiceType() == Guardian.class) {
      return serviceConfiguration.getServiceType().cast(platformMetricService);
    }

    return null;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(IStripeMonitoring.class, Guardian.class);
  }

  @Override
  public void prepareForSynchronization() {
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    StateDumpCollector dump = stateDumpCollector.subStateDumpCollector("metricLogger");
    dump.addState("periodInSeconds", metricLogger.getLoggingSeconds());
    dump.addState("lastLogtime", metricLogger.getLoggingSeconds());
  }

  @Override
  public Map<String, ?> getStateMap() {
    return Map.of("periodInSeconds", metricLogger.getLoggingSeconds(), "lastLogTime", metricLogger.getLastLogTime());
  }

  private void updateLoggingPeriod(Node node) {
    String value = node.getTcProperties().orDefault().getOrDefault(METRIC_LOGGER_PERIOD_PROPERTY, METRIC_LOGGER_PERIOD_DEFAULT);
    try {
      metricLogger.setLoggingPeriod(Integer.parseInt(value));
    } catch (RuntimeException e) {
      LOGGER.error("Unable to set logging period to: " + value, e);
      metricLogger.setLoggingPeriod(Integer.parseInt(METRIC_LOGGER_PERIOD_DEFAULT));
    }
  }

  private static <T> T getService(PlatformConfiguration platformConfiguration, Class<T> type) {
    return Optional.of(platformConfiguration.getExtendedConfiguration(type)).map(services -> switch (services.size()) {
      case 0 -> null;
      case 1 -> services.iterator().next(); // could be null
      default -> throw new IllegalArgumentException("Multiple instances of service " + type + " found");
    }).orElseThrow(() -> new IllegalArgumentException("No non-null instance of service " + type + " found"));
  }
}
