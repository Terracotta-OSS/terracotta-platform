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
package org.terracotta.dynamic_config.cli.config_tool.parsing;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.command.LocalMainJCommanderCommand;
import org.terracotta.dynamic_config.cli.api.command.RemoteConfig;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

@Parameters(commandNames = LocalMainJCommanderCommand.NAME)
public class RemoteMainJCommanderCommand extends LocalMainJCommanderCommand {

  @Parameter(names = {"-entity-request-timeout", "-er", "--entity-request-timeout"}, hidden = true, description = "Entity operation timeout. Default: 120s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> entityOperationTimeout;

  @Parameter(names = {"-request-timeout", "-r", "--request-timeout"}, description = "Request timeout. Default: 10s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> requestTimeout = Measure.of(10, TimeUnit.SECONDS);

  @Parameter(names = {"-connection-timeout", "-t", "--connection-timeout"}, description = "Connection timeout. Default: 10s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> connectionTimeout = Measure.of(10, TimeUnit.SECONDS);

  @Parameter(names = {"-security-root-directory", "-srd", "--security-root-directory"}, description = "Security root directory")
  private String securityRootDirectory;

  @Parameter(names = {"-lock-token", "--lock-token"}, hidden = true, description = "Lock token")
  private String lockToken;

  private final RemoteConfig underlying = new RemoteConfig();

  @Override
  public void validate() {
    if (entityOperationTimeout == null) {
      entityOperationTimeout = requestTimeout.multiply(12);
    }
    underlying.setVerbose(verbose);
    underlying.setConnectionTimeout(connectionTimeout);
    underlying.setSecurityDirectory(securityRootDirectory);
    underlying.setLockToken(lockToken);
    underlying.setEntityOperationTimeout(entityOperationTimeout);
    underlying.setRequestTimeout(requestTimeout);
  }

  @Override
  public void run() {
    underlying.run();
  }

  @Override
  public RemoteConfig getCommand() {
    return underlying;
  }
}
