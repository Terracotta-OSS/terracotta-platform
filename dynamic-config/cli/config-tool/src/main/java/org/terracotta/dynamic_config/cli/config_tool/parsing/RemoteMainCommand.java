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
package org.terracotta.dynamic_config.cli.config_tool.parsing;

import com.beust.jcommander.Parameter;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.api.command.Configuration;
import org.terracotta.dynamic_config.cli.command.LocalMainCommand;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

public class RemoteMainCommand extends LocalMainCommand {

  @Parameter(names = {"-entity-request-timeout", "-er", "--entity-request-timeout"}, hidden = true, description = "Entity operation timeout. Default: 120s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> entityOperationTimeout = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-request-timeout", "-r", "--request-timeout"}, description = "Request timeout. Default: 30s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> requestTimeout = Measure.of(30, TimeUnit.SECONDS);

  @Parameter(names = {"-connect-timeout", "-connection-timeout", "-t", "--connection-timeout"}, description = "Connection timeout. Default: 30s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> connectionTimeout = Measure.of(30, TimeUnit.SECONDS);

  @Parameter(names = {"-security-dir", "-security-root-directory", "-srd", "--security-root-directory"}, description = "Security root directory")
  private String securityRootDirectory;

  @Parameter(names = {"-lock-token", "--lock-token"}, hidden = true, description = "Lock token")
  private String lockToken;

  private final Configuration configuration;

  public RemoteMainCommand(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void run() {
    super.run();
    getConfiguration().setConnectionTimeout(connectionTimeout);
    getConfiguration().setSecurityDirectory(securityRootDirectory);
    getConfiguration().setLockToken(lockToken);
    getConfiguration().setEntityOperationTimeout(entityOperationTimeout);
    getConfiguration().setRequestTimeout(requestTimeout);
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}
