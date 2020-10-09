/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.cli.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.LoggerFactory;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

@Parameters(commandNames = LocalMainCommand.NAME)
public class LocalMainCommand extends JCommanderCommand {
  public static final String NAME = "main";

  @Parameter(names = {"-verbose", "-v", "--verbose"}, description = "Verbose mode. Default: false")
  private boolean verbose = false;

  @Override
  public void run() {
    Logger rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);

    if (verbose) {
      ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) rootLogger.getAppender("STDOUT");
      PatternLayoutEncoder ple = new PatternLayoutEncoder();
      ple.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p %c{1}:%L - %msg%n");
      ple.setContext(appender.getContext());
      ple.start();

      appender.setEncoder(ple);
      rootLogger.setLevel(Level.TRACE);
      rootLogger.getLoggerContext().getLoggerList().forEach(logger -> logger.setLevel(Level.TRACE));
    }
  }

  public boolean isVerbose() {
    return verbose;
  }

  @Override
  public Command getCommand() {
    return null;
  }
}
