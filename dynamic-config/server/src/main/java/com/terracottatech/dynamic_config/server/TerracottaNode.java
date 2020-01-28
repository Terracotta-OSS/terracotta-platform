/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server;

import com.terracottatech.dynamic_config.server.parsing.CustomJCommander;
import com.terracottatech.dynamic_config.server.parsing.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerracottaNode {
  private static final Logger LOGGER = LoggerFactory.getLogger(TerracottaNode.class);

  public static void main(String[] args) {
    Options command = new Options();
    CustomJCommander jCommander = new CustomJCommander("start-node", command);
    try {
      jCommander.parse(args);
      command.process(jCommander);
    } catch (Throwable e) {
      LOGGER.error("Error:", e);
    }
  }
}
