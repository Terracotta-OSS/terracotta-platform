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
package org.terracotta.dynamic_config.cli.config_converter.xml;


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
import org.terracotta.lease.service.config.LeaseConfigurationParser;
import org.terracotta.lease.service.config.LeaseElement;
import org.terracotta.offheapresource.OffHeapResourceConfigurationParser;
import org.terracotta.offheapresource.config.ResourceType;
import org.w3c.dom.Element;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class CommonMapper {

  private static final String WILDCARD_IP = "0.0.0.0";

  private Map<String, ExtendedConfigParser> configParsers = new HashMap<>();
  private Map<String, ServiceConfigParser> serviceConfigParsers = new HashMap<>();

  public CommonMapper(ClassLoader classLoader) {
    for (ExtendedConfigParser parser : ServiceLoader.load(ExtendedConfigParser.class, classLoader)) {
      configParsers.put(parser.getNamespace().toString(), parser);
    }
    for (ServiceConfigParser parser : ServiceLoader.load(ServiceConfigParser.class, classLoader)) {
      serviceConfigParsers.put(parser.getNamespace().toString(), parser);
    }
  }

  public String moreRestrictive(String specific, String general) {
    // same behavior as in com.tc.config.ServerConfiguration
    if (WILDCARD_IP.equals(specific) && !WILDCARD_IP.equals(general)) {
      return general;
    }
    return specific;
  }

  public Map<String, String> toProperties(TcConfig xmlTcConfig) {
    Map<String, String> properties = new HashMap<>();
    TcProperties tcProperties = xmlTcConfig.getTcProperties();
    if (tcProperties != null) {
      tcProperties.getProperty().forEach(p -> properties.put(p.getName(), p.getValue()));
    }
    return properties;
  }

  public Measure<TimeUnit> toClientLeaseDuration(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(LeaseElement.class, Collections.emptyList())
        .stream()
        .map(LeaseElement.class::cast)
        .map(leaseElement -> Measure.of(Long.parseLong(leaseElement.getLeaseValue()), TimeUnit.valueOf(leaseElement.getTimeUnit().toUpperCase())))
        .findFirst()
        .orElse(null);
  }

  public Map<String, Path> toDataDirs(Map<Class<?>, List<Object>> plugins, Predicate<DataRootMapping> filter) {
    return plugins.getOrDefault(DataRootMapping.class, Collections.emptyList())
        .stream()
        .map(DataRootMapping.class::cast)
        .filter(filter)
        .collect(toMap(DataRootMapping::getName, mapping -> Paths.get(mapping.getValue())));
  }

  public Map<String, Measure<MemoryUnit>> toOffheapResources(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(ResourceType.class, Collections.emptyList())
        .stream()
        .map(ResourceType.class::cast)
        .collect(toMap(ResourceType::getName, r -> Measure.of(r.getValue().longValue(), MemoryUnit.parse(r.getUnit().value()))));
  }

  public FailoverPriority toFailoverPriority(org.terracotta.config.FailoverPriority failoverPriority) {
    return failoverPriority == null ?
        FailoverPriority.availability() :
        failoverPriority.getConsistency() != null ?
            failoverPriority.getConsistency().getVoter() == null ?
                FailoverPriority.consistency() :
                FailoverPriority.consistency(failoverPriority.getConsistency().getVoter().getCount()) :
            FailoverPriority.availability();
  }

  public Map<Class<?>, List<Object>> parsePlugins(String xml, TcConfig tcConfig) {
    return parsePlugins(xml, tcConfig, (p, e) -> Optional.empty(), (p, e) -> Optional.empty());
  }

  public Map<Class<?>, List<Object>> parsePlugins(String xml, TcConfig tcConfig,
                                                  BiFunction<ExtendedConfigParser, Element, Optional<Stream<?>>> configMapper,
                                                  BiFunction<ServiceConfigParser, Element, Optional<Stream<?>>> serviceMapper) {
    return tcConfig
        .getPlugins()
        .getConfigOrService()
        .stream()
        .flatMap(o -> parsePlugin(xml, o, configMapper, serviceMapper))
        .collect(groupingBy(Object::getClass));
  }

  protected Stream<?> parsePlugin(String xml, Object o,
                                  BiFunction<ExtendedConfigParser, Element, Optional<Stream<?>>> configMapper,
                                  BiFunction<ServiceConfigParser, Element, Optional<Stream<?>>> serviceMapper) {
    if (o instanceof Config) {
      Element element = ((Config) o).getConfigContent();
      ExtendedConfigParser parser = configParsers.get(element.getNamespaceURI());
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
      ServiceConfigParser parser = serviceConfigParsers.get(element.getNamespaceURI());
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

  public Optional<Map.Entry<String, Path>> toNodeMetadataDir(Map<Class<?>, List<Object>> plugins) {
    // First try to find a deprecated service tag "<persistence:platform-persistence data-directory-id="root1"/>"
    // that will give us the ID of the dataroot to use for platform persistence
    return toDataDirs(plugins, DataRootMapping::isUseForPlatform).entrySet().stream().findFirst();
  }
}