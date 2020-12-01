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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.cli.api.command.Command;
import org.terracotta.dynamic_config.cli.api.command.LocalConfig;

@Parameters(commandNames = LocalMainJCommanderCommand.NAME)
public class LocalMainJCommanderCommand extends JCommanderCommand {
  public static final String NAME = "main";

  @Parameter(names = {"-verbose", "-v", "--verbose"}, description = "Verbose mode. Default: false")
  public boolean verbose = false;

  private final LocalConfig underlying = new LocalConfig();

  @Override
  public void run() {
    underlying.setVerbose(verbose);
    underlying.run();
  }

  @Override
  public Command getCommand() {
    return underlying;
  }
}
