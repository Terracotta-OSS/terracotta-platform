/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config;

import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.diagnostic.LicensingServiceImpl;
import com.terracottatech.dynamic_config.parsing.CustomJCommander;
import com.terracottatech.dynamic_config.parsing.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    Options command = new Options();
    CustomJCommander jCommander = new CustomJCommander("start-node", command);
    try {
      jCommander.parse(args);

      // services
      LOGGER.info("Registering LicensingService with DiagnosticServices");
      LicensingService licensingService = new LicensingServiceImpl();
      DiagnosticServices.register(LicensingService.class, licensingService);

      command.process(jCommander, licensingService);
    } catch (Throwable e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.error(e.getMessage(), e);
      } else {
        LOGGER.error(e.getMessage() + System.lineSeparator());
      }
    }
  }
}
