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
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.LoggerFactory;

@Parameters(commandNames = LocalMainCommand.NAME)
public class LocalMainCommand extends Command {
  public static final String NAME = "main";

  @Parameter(names = {"-v", "--verbose"}, description = "Verbose mode. Default: false")
  private boolean verbose = false;

  @Override
  public void run() {
    Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    if (verbose) {
      rootLogger.setLevel(Level.INFO);
      rootLogger.getLoggerContext().getLoggerList().forEach(logger -> {
        logger.detachAppender("STDOUT");
        logger.detachAppender("STDERR");
      });
      rootLogger.getLoggerContext().getLoggerList()
          .stream()
          .filter(logger -> logger.getName().startsWith("org.terracotta") || logger.getName().startsWith("com.terracottatech") || logger.getName().startsWith("com.tc"))
          .forEach(logger -> logger.setLevel(Level.TRACE));

    } else {
      rootLogger.getLoggerContext().getLoggerList().forEach(logger -> {
        logger.detachAppender("VERBOSE-STDOUT");
        logger.detachAppender("VERBOSE-STDERR");
      });
    }
  }

  public boolean isVerbose() {
    return verbose;
  }
}
