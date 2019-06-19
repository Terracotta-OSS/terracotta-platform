/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.beust.jcommander.Parameter;
import org.slf4j.LoggerFactory;

public class MainCommand extends AbstractCommand {
  public static final String NAME = "main";

  @Parameter(names = {"-v", "--verbose"}, description = "Verbose mode")
  private boolean verbose = false;

  @Parameter(names = {"-r", "--request-timeout"}, description = "Request timeout in milliseconds")
  private String requestTimeout;

  @Parameter(names = {"-t", "--connection-timeout"}, description = "Connection timeout in milliseconds")
  private String connectionTimeout;

  @Parameter(names = {"-srd", "--security-root-directory"}, description = "Security root directory")
  private String securityRootDirectory;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void run() {
    if (verbose) {
      Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(Level.DEBUG);
      Appender<ILoggingEvent> detailAppender = rootLogger.getAppender("STDOUT-DETAIL");

      Logger configToolLogger = (Logger) LoggerFactory.getLogger("com.terracottatech.dynamic_config.cli");
      configToolLogger.setLevel(Level.DEBUG);
      //Detach the STDOUT appender which logs in a minimal pattern and attached STDOUT-DETAIL appender
      configToolLogger.detachAppender("STDOUT");
      configToolLogger.addAppender(detailAppender);
    }
  }

  @Override
  public String usage() {
    return "";
  }

  @Override
  public void validate() {
    // Do nothing
  }
}
