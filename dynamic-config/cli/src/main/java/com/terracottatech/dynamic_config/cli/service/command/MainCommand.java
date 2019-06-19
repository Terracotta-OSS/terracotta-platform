/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.LongConverter;
import org.slf4j.LoggerFactory;

@Parameters(commandNames = "main")
public class MainCommand extends Command {

  @Parameter(names = {"-v", "--verbose"}, description = "Verbose mode")
  private boolean verbose = false;

  @Parameter(names = {"-r", "--request-timeout"}, description = "Request timeout in milliseconds", converter = LongConverter.class)
  private long requestTimeout = 10_000;

  @Parameter(names = {"-t", "--connection-timeout"}, description = "Connection timeout in milliseconds", converter = LongConverter.class)
  private long connectionTimeout = 30_0000;

  @Parameter(names = {"-srd", "--security-root-directory"}, description = "Security root directory")
  private String securityRootDirectory;

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
  public void validate() {
    // Do nothing
  }

  public boolean isVerbose() {
    return verbose;
  }

  public long getRequestTimeout() {
    return requestTimeout;
  }

  public long getConnectionTimeout() {
    return connectionTimeout;
  }

  public String getSecurityRootDirectory() {
    return securityRootDirectory;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void setRequestTimeout(long requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public void setConnectionTimeout(long connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public void setSecurityRootDirectory(String securityRootDirectory) {
    this.securityRootDirectory = securityRootDirectory;
  }
}
