/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.diagnostic;

import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.LICENSE_FILE_NAME;

public class LicensingServiceImpl implements LicensingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(LicensingServiceImpl.class);

  @Override
  public void installLicense(String licenseContent) {
    Path licenseDir = NomadBootstrapper.getNomadRepositoryManager().getLicensePath();
    if (licenseDir == null) {
      throw new IllegalStateException("Nomad license path should not be null at this stage");
    }

    try {
      Files.write(licenseDir.resolve(LICENSE_FILE_NAME), licenseContent.getBytes(StandardCharsets.UTF_8));
      LOGGER.info("License file: {} successfully copied to: {}", LICENSE_FILE_NAME, licenseDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void updateLicense(String licenseContent) {
    //TODO [DYNAMIC-CONFIG]: TRACK #2 : SUPPORT LICENSE UPDATE
    throw new UnsupportedOperationException("TODO Implement me!");
  }
}
