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
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.cli.command.Usage;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "unset", commandDescription = "Unset configuration properties")
@Usage("unset -s <hostname[:port]> -c <[namespace:]property>,<[namespace:]property>...")
public class UnsetCommand extends ConfigurationMutationCommand {
  public UnsetCommand() {
    super(Operation.UNSET);
  }
}
