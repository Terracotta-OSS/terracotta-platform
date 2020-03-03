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
package org.terracotta.dynamic_config.xml;


import org.slf4j.event.Level;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.config.Config;
import org.terracotta.config.Service;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcProperties;
import org.terracotta.config.data_roots.DataRootConfigParser;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.config.service.ServiceConfigParser;
import org.terracotta.data.config.DataRootMapping;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.Logger;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcNode;
import org.terracotta.lease.service.config.LeaseConfigurationParser;
import org.terracotta.lease.service.config.LeaseElement;
import org.terracotta.offheapresource.OffHeapResourceConfigurationParser;
import org.terracotta.offheapresource.config.ResourceType;
import org.w3c.dom.Element;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class MapperUtils {

  private static Map<String, ExtendedConfigParser> CONFIG_PARSERS = new HashMap<>();
  private static Map<String, ServiceConfigParser> SERVICE_PARSERS = new HashMap<>();

  static {
    for (ExtendedConfigParser parser : ServiceLoader.load(ExtendedConfigParser.class, MapperUtils.class.getClassLoader())) {
      CONFIG_PARSERS.put(parser.getNamespace().toString(), parser);
    }
    for (ServiceConfigParser parser : ServiceLoader.load(ServiceConfigParser.class, MapperUtils.class.getClassLoader())) {
      SERVICE_PARSERS.put(parser.getNamespace().toString(), parser);
    }
  }

  private static final String WILDCARD_IP = "0.0.0.0";

  public static String moreRestrictive(String specific, String general) {
    // same behavior as in com.tc.config.ServerConfiguration
    if (WILDCARD_IP.equals(specific) && !WILDCARD_IP.equals(general)) {
      return general;
    }
    return specific;
  }

  public static Map<String, String> toProperties(TcConfig xmlTcConfig) {
    Map<String, String> properties = new HashMap<>();
    TcProperties tcProperties = xmlTcConfig.getTcProperties();
    if (tcProperties != null) {
      tcProperties.getProperty().forEach(p -> properties.put(p.getName(), p.getValue()));
    }
    return properties;
  }

  public static Map<String, Level> toLoggers(TcNode xmlNode) {
    return Optional.ofNullable(xmlNode.getLoggerOverrides())
        .map(loggers -> loggers.getLogger()
            .stream()
            .collect(toMap(
                Logger::getName,
                logger -> Level.valueOf(logger.getLevel().name()),
                (level1, level2) -> level2,
                LinkedHashMap::new)))
        .orElseGet(() -> new LinkedHashMap<>(0));
  }

  public static Measure<TimeUnit> toClientLeaseDuration(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(LeaseElement.class, Collections.emptyList())
        .stream()
        .map(LeaseElement.class::cast)
        .map(leaseElement -> Measure.of(Long.parseLong(leaseElement.getLeaseValue()), TimeUnit.valueOf(leaseElement.getTimeUnit().toUpperCase())))
        .findFirst()
        .orElse(null);
  }

  public static Map<String, Path> toDataDirs(Map<Class<?>, List<Object>> plugins, Predicate<DataRootMapping> filter) {
    return plugins.getOrDefault(DataRootMapping.class, Collections.emptyList())
        .stream()
        .map(DataRootMapping.class::cast)
        .filter(filter)
        .collect(toMap(DataRootMapping::getName, mapping -> Paths.get(mapping.getValue())));
  }

  public static Map<String, Measure<MemoryUnit>> toOffheapResources(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(ResourceType.class, Collections.emptyList())
        .stream()
        .map(ResourceType.class::cast)
        .collect(toMap(ResourceType::getName, r -> Measure.of(r.getValue().longValue(), MemoryUnit.parse(r.getUnit().value()))));
  }

  public static FailoverPriority toFailoverPriority(org.terracotta.config.FailoverPriority failoverPriority) {
    requireNonNull(failoverPriority);
    return failoverPriority.getConsistency() != null ?
        failoverPriority.getConsistency().getVoter() == null ? FailoverPriority.consistency() :
            FailoverPriority.consistency(failoverPriority.getConsistency().getVoter().getCount()) :
        FailoverPriority.availability();
  }

  public static Map<Class<?>, List<Object>> parsePlugins(String xml, TcConfig tcConfig) {
    return parsePlugins(xml, tcConfig, (p, e) -> Optional.empty(), (p, e) -> Optional.empty());
  }

  public static Map<Class<?>, List<Object>> parsePlugins(String xml, TcConfig tcConfig,
                                                         BiFunction<ExtendedConfigParser, Element, Optional<Stream<?>>> configMapper,
                                                         BiFunction<ServiceConfigParser, Element, Optional<Stream<?>>> serviceMapper) {
    return tcConfig
        .getPlugins()
        .getConfigOrService()
        .stream()
        .flatMap(o -> parsePlugin(xml, o, configMapper, serviceMapper))
        .collect(groupingBy(Object::getClass));
  }

  private static Stream<?> parsePlugin(String xml, Object o,
                                       BiFunction<ExtendedConfigParser, Element, Optional<Stream<?>>> configMapper,
                                       BiFunction<ServiceConfigParser, Element, Optional<Stream<?>>> serviceMapper) {
    if (o instanceof Config) {
      Element element = ((Config) o).getConfigContent();
      ExtendedConfigParser parser = CONFIG_PARSERS.get(element.getNamespaceURI());
      // xml is expected to be valid
      if (parser == null) {
        throw new AssertionError("ExtendedConfigParser not found for namespace " + element.getNamespaceURI());
      }
      // special handling for data root to not apply any defaults
      if (parser instanceof DataRootConfigParser) {
        return ((DataRootConfigParser) parser).parser().apply(element).getDirectory().stream();
      }
      // special handling for offheaps to not apply any defaults
      if (parser instanceof OffHeapResourceConfigurationParser) {
        return ((OffHeapResourceConfigurationParser) parser).parser().apply(element).getResource().stream();
      }
      // delegate parsing to caller
      return configMapper.apply(parser, element)
          .orElseGet(() -> Stream.of(parser.parse(element, xml))); // default case (includes Cluster tag)

    } else if (o instanceof Service) {
      Element element = ((Service) o).getServiceContent();
      ServiceConfigParser parser = SERVICE_PARSERS.get(element.getNamespaceURI());
      // xml is expected to be valid
      if (parser == null) {
        throw new AssertionError("ServiceConfigParser not found for namespace " + element.getNamespaceURI());
      }
      // lease special handling
      if (parser instanceof LeaseConfigurationParser) {
        return Stream.of(((LeaseConfigurationParser) parser).parser().apply(element));
      }

      // delegate parsing to caller
      return serviceMapper.apply(parser, element)
          .orElseGet(() -> Stream.of(parser.parse(element, xml))); // default case (includes FRSPersistenceConfigurationParser)

    } else {
      throw new AssertionError("Unsupported type: " + o.getClass());
    }
  }
}