/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config;

import com.beust.jcommander.JCommander;
import com.terracottatech.dynamic_config.config.Options;
import com.terracottatech.dynamic_config.parsing.PrettyUsagePrintingJCommander;

public class Main {
  public static void main(String[] args) {
    Options command = new Options();
    JCommander jCommander = new PrettyUsagePrintingJCommander("start-node", command);
    jCommander.parse(args);
    command.process(jCommander);
  }
}
