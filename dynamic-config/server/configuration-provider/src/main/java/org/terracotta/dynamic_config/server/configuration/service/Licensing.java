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
  private static final String DATAHUB_LICENSE_FLAG = "DatahubLicense";
  // guard access to the installed license
  private final ReadWriteLock licenseLock = new ReentrantReadWriteLock();

  private final LicenseService licenseService;
  private final Path licenseFile;
  private volatile String unconfiguredLicenseContent; // set only when the server is unconfigured (RO mode)
  private final NomadServerManager nomadServerManager;

  public Licensing(LicenseService licenseService, NomadServerManager nomadServerManager) {
    this.licenseService = requireNonNull(licenseService);
    this.nomadServerManager = nomadServerManager;
    this.licenseFile = requireNonNull(nomadServerManager.getConfigurationManager().getLicensePath()).resolve(LICENSE_FILE_NAME);
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

  public Optional<String> getLicenseContent() {
    return Optional.ofNullable(read().orElseGet(() -> getUnconfiguredLicenseContent().orElse(null)));
  }

  /**
   * Only return the unconfiguredLicenseContent if the server is unconfigured (i.e. RO mode).
   */
  private Optional<String> getUnconfiguredLicenseContent() {
    return nomadServerManager.getNomadMode() == NomadMode.RO && unconfiguredLicenseContent != null ?
        Optional.of(unconfiguredLicenseContent) : Optional.empty();
  }

  private Optional<String> read() throws UncheckedIOException, IllegalStateException {
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

  /**
   * Licenses can be configured (set/unset) when the server is ACTIVE or UNCONFIGURED.
   * Since the config folder does not yet exist when the server is UNCONFIGURED, we only 'install' the license
   * (i.e. copy the specified file to the config folder) when the server is ACTIVE.
   * If install is called when the server is UNCONFIGURED (RO mode), we simply update the unconfiguredLicenseContent
   * member with the supplied licenseContent.
   * If install is called when the server is CONFIGURED (RW mode), we use the license that was supplied during activation.
   * If a license was not specified during activation, we use the unconfiguredLicenseContent that was set when the
   * server was UNCONFIGURED (if one was specified).
   */
  public void install(String licenseContent, Cluster cluster) {
    licenseLock.writeLock().lock();
    try {
      if (nomadServerManager.getNomadMode() == NomadMode.RO) {
        // server is unconfigured (or in Repair)
        unconfiguredLicenseContent = licenseContent;
      } else {
        // server is configured
        if (licenseContent == null && unconfiguredLicenseContent != null) {
          // no license specified; if available, use the license set when unconfigured
          licenseContent = unconfiguredLicenseContent;
        }
        if (licenseContent != null) {
          // only perform file operations on activated servers (config folders do not exist when unconfigured)
          try {
            final Path tempFile = Files.createTempFile("terracotta-license-", ".xml");
            try {
              Files.write(tempFile, licenseContent.getBytes(StandardCharsets.UTF_8));
              License license = licenseService.parse(tempFile);
              LOGGER.info("Validating license");
              LOGGER.info(license.toLoggingString());
              licenseService.validate(tempFile, cluster);
              LOGGER.info("License validated");
              if (license.getType().equals(DATAHUB_LICENSE_FLAG)) {
                LOGGER.info("This Terracotta cluster is licensed for use only with webMethods DataHub");
              }
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
        }
      }
    } finally {
      licenseLock.writeLock().unlock();
    }
  }

  public void uninstall() {
    licenseLock.writeLock().lock();
    try {
      unconfiguredLicenseContent = null;
      if (isInstalled()) {
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
