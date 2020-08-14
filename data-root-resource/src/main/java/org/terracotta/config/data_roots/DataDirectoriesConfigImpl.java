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
package org.terracotta.config.data_roots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.data_roots.management.DataRootBinding;
import org.terracotta.config.data_roots.management.DataRootSettingsManagementProvider;
import org.terracotta.config.data_roots.management.DataRootStatisticsManagementProvider;
import org.terracotta.data.config.DataRootMapping;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.ManageableServerComponent;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author vmad
 */
public class DataDirectoriesConfigImpl implements DataDirectoriesConfig, ManageableServerComponent, StateDumpable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataDirectoriesConfigImpl.class);

  private final ConcurrentMap<String, Path> dataRootMap = new ConcurrentHashMap<>();
  private final String platformRootIdentifier;
  private final ConcurrentMap<String, DataDirectories> serverToDataRoots = new ConcurrentHashMap<>();
  private final IParameterSubstitutor parameterSubstitutor;
  private final PathResolver pathResolver;
  private final Collection<EntityManagementRegistry> registries = new CopyOnWriteArrayList<>();

  public DataDirectoriesConfigImpl(IParameterSubstitutor parameterSubstitutor, PathResolver pathResolver, Path metadataDir, Map<String, Path> dataDirectories) {
    this.parameterSubstitutor = parameterSubstitutor;
    this.pathResolver = pathResolver;

    //add data dirs
    dataDirectories.forEach((name, path) -> addDataDirectory(name, path.toString()));

    // add platform metadata dir first
    if (metadataDir == null) {
      this.platformRootIdentifier = null;

    } else {
      // backward compat': it was possible to define the same data root id for both platform persistence and user entities...
      // so we need to search if we have a data dir that contains the metadataDir
      // otherwise, we are using dynamic config and we would generate an ID.
      this.platformRootIdentifier = dataDirectories.entrySet()
          .stream()
          .filter(e -> e.getValue().equals(metadataDir))
          .map(Map.Entry::getKey)
          .findAny()
          .orElseGet(() -> {
            // we are using dynamic config
            String id = "platform";
            if (dataDirectories.containsKey(id)) {
              id += "-" + System.currentTimeMillis();
            }
            addDataDirectory(id, metadataDir.toString());
            return id;
          });
    }
  }

  public DataDirectoriesConfigImpl(IParameterSubstitutor parameterSubstitutor, PathResolver pathResolver, org.terracotta.data.config.DataDirectories dataDirectories) {
    this(parameterSubstitutor, pathResolver, dataDirectories, false);
  }

  public DataDirectoriesConfigImpl(IParameterSubstitutor parameterSubstitutor, PathResolver pathResolver, org.terracotta.data.config.DataDirectories dataDirectories, boolean skipIO) {
    this.parameterSubstitutor = parameterSubstitutor;
    this.pathResolver = pathResolver;

    String tempPlatformRootIdentifier = null;
    for (DataRootMapping mapping : dataDirectories.getDirectory()) {
      addDataDirectory(mapping.getName(), mapping.getValue(), skipIO);
      if (mapping.isUseForPlatform()) {
        if (tempPlatformRootIdentifier == null) {
          tempPlatformRootIdentifier = mapping.getName();
        } else {
          throw new DataDirectoriesConfigurationException("More than one data directory is configured to be used by platform");
        }
      }
    }
    platformRootIdentifier = tempPlatformRootIdentifier;
  }


  @Override
  public DataDirectories getDataDirectoriesForServer(PlatformConfiguration platformConfiguration) {
    return getDataRootsForServer(platformConfiguration.getServerName());
  }

  @Override
  public void addDataDirectory(String name, String path) {
    addDataDirectory(name, path, false);
  }

  public void addDataDirectory(String name, String path, boolean skipIO) {
    validateDataDirectory(name, path, skipIO);

    Path dataDirectory = compute(Paths.get(path));

    // with dynamic config, XML is parsed multiple times during the lifecycle of the server and these logs are triggered at each parsing
    LOGGER.debug("Defined directory with name: {} at location: {}", name, dataDirectory);

    dataRootMap.put(name, dataDirectory);

    for (EntityManagementRegistry registry : registries) {
      registry.registerAndRefresh(new DataRootBinding(name, dataDirectory));
    }
  }

  @Override
  public void validateDataDirectory(String name, String path) {
    validateDataDirectory(name, path, false);
  }

  public void validateDataDirectory(String name, String path, boolean skipIO) {
    Path dataDirectory = compute(Paths.get(path));

    if (dataRootMap.containsKey(name)) {
      throw new DataDirectoriesConfigurationException("A data directory with name: " + name + " already exists");
    }

    Path overlapPath = overLapsWith(dataDirectory);
    if (overlapPath != null) {
      throw new DataDirectoriesConfigurationException(
          String.format(
              "Path for data directory: %s overlaps with the existing data directory path: %s",
              dataDirectory,
              overlapPath
          )
      );
    }

    if (!skipIO) {
      try {
        ensureDirectory(dataDirectory);
      } catch (IOException e) {
        throw new RuntimeException(e.toString(), e);
      }
    }
  }

  @Override
  public void onManagementRegistryCreated(EntityManagementRegistry registry) {
    long consumerId = registry.getMonitoringService().getConsumerId();
    LOGGER.trace("[{}] onManagementRegistryCreated()", consumerId);

    registries.add(registry);

    registry.addManagementProvider(new DataRootSettingsManagementProvider());
    registry.addManagementProvider(new DataRootStatisticsManagementProvider(this));

    DataDirectories dataDirectories = getDataRootsForServer(registry.getMonitoringService().getServerName());

    for (String identifier : dataDirectories.getDataDirectoryNames()) {
      LOGGER.trace("[{}] onManagementRegistryCreated() - Exposing DataDirectory:{}", consumerId, identifier);
      registry.register(new DataRootBinding(identifier, dataDirectories.getDataDirectory(identifier)));
    }

    registry.refresh();
  }

  @Override
  public void onManagementRegistryClose(EntityManagementRegistry registry) {
    registries.remove(registry);
  }

  @Override
  public void addStateTo(StateDumpCollector dump) {
    for (Map.Entry<String, Path> entry : dataRootMap.entrySet()) {
      StateDumpCollector pathDump = dump.subStateDumpCollector(entry.getKey());
      pathDump.addState("path", entry.getValue().toString());
      pathDump.addState("totalDiskUsage", String.valueOf(getDiskUsageByRootIdentifier(entry.getKey())));
    }
  }

  @Override
  public void close() throws IOException {
    for (DataDirectories dataDirectories : serverToDataRoots.values()) {
      dataDirectories.close();
    }
  }

  Path getRoot(String identifier) {
    if (identifier == null) {
      throw new NullPointerException("Data directory name is null");
    }

    if (!dataRootMap.containsKey(identifier)) {
      throw new IllegalArgumentException(String.format("Data directory with name: %s is not present in server's configuration", identifier));
    }

    return dataRootMap.get(identifier);
  }

  Optional<String> getPlatformRootIdentifier() {
    return Optional.ofNullable(platformRootIdentifier);
  }

  Set<String> getRootIdentifiers() {
    return Collections.unmodifiableSet(dataRootMap.keySet());
  }

  public long getDiskUsageByRootIdentifier(String identifier) {
    return computeFolderSize(getRoot(identifier));
  }

  void ensureDirectory(Path directory) throws IOException {
    if (!Files.exists(directory)) {
      Files.createDirectories(directory);
    } else {
      if (!Files.isDirectory(directory)) {
        throw new RuntimeException(directory.getFileName() + " exists under " + directory.getParent() + " but is not a directory");
      }
    }
  }

  private DataDirectories getDataRootsForServer(String serverName) {
    return serverToDataRoots.computeIfAbsent(serverName,
        name -> new DataDirectoriesWithServerName(this, DataDirectoriesConfig.cleanStringForPath(name)));
  }

  private Path compute(Path path) {
    return parameterSubstitutor.substitute(pathResolver.resolve(path)).normalize();
  }

  private Path overLapsWith(Path newDataDirectoryPath) {
    Collection<Path> dataDirectoryPaths = dataRootMap.values();
    for (Path existingDataDirectoryPath : dataDirectoryPaths) {
      if (existingDataDirectoryPath.startsWith(newDataDirectoryPath) || newDataDirectoryPath.startsWith(existingDataDirectoryPath)) {
        return existingDataDirectoryPath;
      }
    }
    return null;
  }

  /**
   * Attempts to calculate the size of a file or directory.
   * Since the operation is non-atomic, the returned value may be inaccurate.
   * However, this method is quick and does its best.
   */
  private static long computeFolderSize(Path path) {
    final AtomicLong size = new AtomicLong(0);
    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          size.addAndGet(attrs.size());
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
          // Skip folders that can't be traversed
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
          // Ignore errors traversing a folder
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
    }
    return size.get();
  }

}