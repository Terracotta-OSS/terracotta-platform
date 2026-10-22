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
import com.tc.text.PrettyPrintable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.model.LogicalServerState;
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
import org.terracotta.metrics.MetricService;
import org.terracotta.platform.metrics.log.MetricLogger;
import org.terracotta.platform.metrics.log.MetricLoggingTrigger;
import org.terracotta.platform.metrics.platform.DefaultPlatformMetricService;
import org.terracotta.platform.metrics.voltron.DefaultVoltronMetricCollector;
import org.terracotta.platform.metrics.voltron.DelegateEventService;
import org.terracotta.server.Server;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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

  // our metric collector (implements MetricService and EventService)
  private final DefaultVoltronMetricCollector metricCollector = new DefaultVoltronMetricCollector();

  private UID thisNodeUID;
  private MetricLogger metricLogger;
  private EventService eventService;
  private EventRegistration eventListener;


  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    Server server = getService(platformConfiguration, Server.class);
    TopologyService topologyService = getService(platformConfiguration, TopologyService.class);
    DynamicConfigEventService dynamicConfigEventService = getService(platformConfiguration, DynamicConfigEventService.class);

    // grab current node info
    thisNodeUID = topologyService.getRuntimeNodeContext().getNodeUID();

    // our own metric facade (as we understand them)
    DefaultPlatformMetricService platformMetricService = new DefaultPlatformMetricService(server, metricCollector);

    // periodic logger
    metricLogger = new MetricLogger(platformMetricService);

    // event service returned to core.
    // We intercept some events to trigger an immediate logging.
    eventService = new DelegateEventService(metricCollector) {
      @Override
      public void fireEvent(Event event, Map<String, String> map) {
        switch (event) {
          case MEMBER_STATE, PEER_STATE -> {
            // core sends the same state event several times so avoid duplicates
            TreeMap<String, LogicalServerState> states = platformMetricService.getStripeStates();
            super.fireEvent(event, map);
            if (!platformMetricService.getStripeStates().equals(states)) {
              metricLogger.logNow(MetricLoggingTrigger.TOPOLOGY_CHANGE);
            }
          }
          default -> super.fireEvent(event, map);
        }
      }
    };

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
    if (serviceConfiguration.getServiceType() == MetricService.class) {
      return serviceConfiguration.getServiceType().cast(metricCollector);
    }

    if (serviceConfiguration.getServiceType() == EventService.class) {
      return serviceConfiguration.getServiceType().cast(eventService);
    }

    return null;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(MetricService.class, EventService.class);
  }

  @Override
  public void prepareForSynchronization() {
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    metricCollector.addStateTo(stateDumpCollector.subStateDumpCollector("metrics"));
  }

  @Override
  public Map<String, ?> getStateMap() {
    return Collections.singletonMap("metrics", metricCollector.getStateMap());
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
