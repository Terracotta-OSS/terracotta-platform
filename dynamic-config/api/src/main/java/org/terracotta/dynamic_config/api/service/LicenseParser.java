/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.License;

import java.nio.file.Path;

/**
 * @author Mathieu Carbou
 */
public interface LicenseParser {
  License parse(Path file);

  static LicenseParser unsupported() {
    return file -> {
      throw new UnsupportedOperationException("Licensing is not supported");
    };
  }
}
