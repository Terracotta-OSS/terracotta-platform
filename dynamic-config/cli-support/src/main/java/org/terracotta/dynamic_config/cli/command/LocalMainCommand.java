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

  public boolean isVerbose() {
    return verbose;
  }
}
