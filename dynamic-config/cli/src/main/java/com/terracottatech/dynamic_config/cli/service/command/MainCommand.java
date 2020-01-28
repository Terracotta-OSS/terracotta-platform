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
import com.terracottatech.dynamic_config.cli.common.TimeUnitConverter;
import com.terracottatech.dynamic_config.model.Measure;
import com.terracottatech.dynamic_config.model.TimeUnit;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

@Parameters(commandNames = MainCommand.NAME)
public class MainCommand extends Command {
  public static final String NAME = "main";

  @Parameter(names = {"-v", "--verbose"}, description = "Verbose mode. Default: false")
  private boolean verbose = false;

  @Parameter(names = {"-r", "--request-timeout"}, description = "Request timeout. Default: 10s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> requestTimeout = Measure.of(10, TimeUnit.SECONDS);

  @Parameter(names = {"-t", "--connection-timeout"}, description = "Connection timeout. Default: 30s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> connectionTimeout = Measure.of(30, TimeUnit.SECONDS);

  @Parameter(names = {"-srd", "--security-root-directory"}, description = "Security root directory")
  private String securityRootDirectory;

  @Parameter(names = {"-rd", "--restart-delay"}, description = "Restart delay. Default: 2s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Override
  public void run() {
    if (verbose) {
      Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(Level.INFO);
      Appender<ILoggingEvent> detailAppender = rootLogger.getAppender("STDOUT-DETAIL");

      Stream.of(
          "com.terracottatech.dynamic_config",
          "com.terracottatech.nomad",
          "com.terracottatech.persistence.sanskrit"
      ).forEach(name -> {
        Logger logger = (Logger) LoggerFactory.getLogger(name);
        logger.setLevel(Level.TRACE);
        //Detach the STDOUT appender which logs in a minimal pattern and attached STDOUT-DETAIL appender
        logger.detachAppender("STDOUT");
        logger.addAppender(detailAppender);
      });
    }
  }

  @Override
  public void validate() {
    // Do nothing
  }

  public Measure<TimeUnit> getRequestTimeout() {
    return requestTimeout;
  }

  public Measure<TimeUnit> getConnectionTimeout() {
    return connectionTimeout;
  }

  public Measure<TimeUnit> getRestartDelay() {
    return restartDelay;
  }

  public String getSecurityRootDirectory() {
    return securityRootDirectory;
  }
}
