/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Stream;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

public class LocalMainCommand extends Command {

  @Parameter(names = {"-verbose", "-v", "--verbose"}, description = "Verbose mode. Default: false")
  public boolean verbose = false;

  @Override
  public void run() {
    if (verbose) {
      Logger rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);

      ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) Stream.of("STDERR", "STDOUT", "CONSOLE")
          .map(rootLogger::getAppender)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);

      if (appender == null) {
        throw new IllegalStateException("Logging appender 'STDERR' is missing!");
      }
      PatternLayoutEncoder ple = new PatternLayoutEncoder();
      ple.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p %c{1}:%L - %msg%n");
      ple.setContext(appender.getContext());
      ple.start();

      appender.setEncoder(ple);
      rootLogger.setLevel(Level.TRACE);
      rootLogger.getLoggerContext().getLoggerList().forEach(logger -> logger.setLevel(Level.TRACE));
    }
  }
}
