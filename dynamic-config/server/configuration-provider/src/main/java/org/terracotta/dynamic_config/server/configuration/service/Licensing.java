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
package org.terracotta.dynamic_config.server.configuration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.server.api.InvalidLicenseException;
import org.terracotta.dynamic_config.server.api.LicenseService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
class Licensing {

  private static final Logger LOGGER = LoggerFactory.getLogger(Licensing.class);
  private static final String LICENSE_FILE_NAME = "license.xml";

  // guard access to the installed license
  private final ReadWriteLock licenseLock = new ReentrantReadWriteLock();

  private final LicenseService licenseService;
  private final Path licenseFile;

  public Licensing(LicenseService licenseService, Path licenseDir) {
    this.licenseService = requireNonNull(licenseService);
    this.licenseFile = requireNonNull(licenseDir).resolve(LICENSE_FILE_NAME);
  }

  public Path getLicenseFile() {
    return licenseFile;
  }

  public boolean isInstalled() {
    licenseLock.readLock().lock();
    try {
      return licenseFile.toFile().exists() && Files.isRegularFile(licenseFile) && Files.isReadable(licenseFile);
    } finally {
      licenseLock.readLock().unlock();
    }
  }

  public Optional<String> read() throws UncheckedIOException, IllegalStateException {
    licenseLock.readLock().lock();
    try {
      return licenseFile.toFile().exists() ?
          Optional.of(new String(Files.readAllBytes(licenseFile), StandardCharsets.UTF_8)) :
          Optional.empty();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      licenseLock.readLock().unlock();
    }
  }

  public Optional<License> parse() {
    licenseLock.readLock().lock();
    try {
      return isInstalled() ? Optional.of(licenseService.parse(licenseFile)) : Optional.empty();
    } finally {
      licenseLock.readLock().unlock();
    }
  }

  /**
   * @return false is license not installed, true if valid, {@code org.terracotta.dynamic_config.server.api.LicenseService#validate} will throw if invalid
   */
  public boolean validate(Cluster cluster) throws InvalidLicenseException {
    licenseLock.readLock().lock();
    try {
      if (!isInstalled()) {
        LOGGER.warn("Unable to validate cluster against license: license not installed: {}", cluster.toShapeString());
        return false;
      }
      licenseService.validate(licenseFile, cluster);
      LOGGER.debug("License is valid for cluster: {}", cluster.toShapeString());
      return true;
    } finally {
      licenseLock.readLock().unlock();
    }
  }

  public void install(String licenseContent, Cluster cluster) {
    licenseLock.writeLock().lock();
    try {
      if (licenseContent != null) {
        try {
          final Path tempFile = Files.createTempFile("terracotta-license-", ".xml");
          try {
            Files.write(tempFile, licenseContent.getBytes(StandardCharsets.UTF_8));
            licenseService.validate(tempFile, cluster);
            LOGGER.info("License validated");
            LOGGER.debug("Moving license file: {} to: {}", tempFile, licenseFile);
            org.terracotta.utilities.io.Files.relocate(tempFile, licenseFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("License installed");
          } finally {
            try {
              org.terracotta.utilities.io.Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
        LOGGER.info("License installation successful");
      } else if (isInstalled()) {
        try {
          org.terracotta.utilities.io.Files.deleteIfExists(licenseFile);
        } catch (IOException e) {
          throw new UncheckedIOException("Error deleting existing license " + licenseFile + ": " + e.getMessage(), e);
        }
        LOGGER.info("License removal successful");
      } else {
        LOGGER.info("No license installed");
      }
    } finally {
      licenseLock.writeLock().unlock();
    }
  }

}
