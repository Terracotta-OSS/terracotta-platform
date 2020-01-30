/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

@Parameters(commandNames = LocalMainCommand.NAME)
public class LocalMainCommand extends Command {
  public static final String NAME = "main";

  @Parameter(names = {"-v", "--verbose"}, description = "Verbose mode. Default: false")
  private boolean verbose = false;

  @Override
  public void run() {
    if (verbose) {
      Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(Level.INFO);
      Appender<ILoggingEvent> detailAppender = rootLogger.getAppender("STDOUT-DETAIL");

      Stream.of(
          "org.terracotta.dynamic_config",
          "org.terracotta.nomad",
          "org.terracotta.persistence.sanskrit",
          "org.terracotta.diagnostic"
      ).forEach(name -> {
        Logger logger = (Logger) LoggerFactory.getLogger(name);
        logger.setLevel(Level.TRACE);
        //Detach the STDOUT appender which logs in a minimal pattern and attached STDOUT-DETAIL appender
        logger.detachAppender("STDOUT");
        logger.addAppender(detailAppender);
      });
    }
  }
}
