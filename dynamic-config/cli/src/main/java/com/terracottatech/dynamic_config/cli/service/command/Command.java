/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;

/**
 * @author Mathieu Carbou
 */
public abstract class Command implements Runnable {

  @Parameter(names = {"-h", "--help"}, description = "Help", help = true)
  private boolean help;

  public boolean isHelp() {
    return help;
  }

  /**
   * Run extra global validation after each parameter has been parsed, converted, injected and validated
   */
  public void validate() {}

  ;
}
