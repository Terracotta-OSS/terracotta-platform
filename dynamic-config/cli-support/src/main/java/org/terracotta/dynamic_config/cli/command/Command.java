/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.command;

import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mathieu Carbou
 */
public abstract class Command implements Runnable {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Parameter(names = {"-h", "--help"}, description = "Help", help = true)
  private boolean help;

  public boolean isHelp() {
    return help;
  }

  /**
   * Run extra global validation after each parameter has been parsed, converted, injected and validated
   * <p>
   * Optional implementation.
   */
  public void validate() {}
}
