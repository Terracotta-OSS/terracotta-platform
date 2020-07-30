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
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.math.BigInteger;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = LocalMainCommand.NAME)
public class RemoteMainCommand extends LocalMainCommand {

  @Parameter(names = {"-er", "--entity-request-timeout"}, hidden = true, description = "Entity operation timeout. Default: 120s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> entityOperationTimeout;

  @Parameter(names = {"-et", "--entity-connection-timeout"}, hidden = true, description = "Entity Connection timeout. Default: 30s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> entityConnectionTimeout;

  @Parameter(names = {"-r", "--request-timeout"}, description = "Request timeout. Default: 10s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> requestTimeout = Measure.of(10, TimeUnit.SECONDS);

  @Parameter(names = {"-t", "--connection-timeout"}, description = "Connection timeout. Default: 10s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> connectionTimeout = Measure.of(10, TimeUnit.SECONDS);

  @Parameter(names = {"-srd", "--security-root-directory"}, description = "Security root directory")
  private String securityRootDirectory;

  @Parameter(names = {"--lock-token"}, hidden = true, description = "Lock token")
  private String lockToken;

  public Measure<TimeUnit> getRequestTimeout() {
    return requestTimeout;
  }

  public Measure<TimeUnit> getConnectionTimeout() {
    return connectionTimeout;
  }

  public Measure<TimeUnit> getEntityOperationTimeout() {
    return entityOperationTimeout;
  }

  public Measure<TimeUnit> getEntityConnectionTimeout() {
    return entityConnectionTimeout;
  }

  public String getSecurityRootDirectory() {
    return securityRootDirectory;
  }

  public String getLockToken() {
    return lockToken;
  }

  @Override
  public void validate() {
    super.validate();
    if (entityOperationTimeout == null) {
      entityOperationTimeout = Measure.of(
          requestTimeout.getExactQuantity().multiply(BigInteger.valueOf(12)),
          requestTimeout.getUnit());
    }
    if (entityConnectionTimeout == null) {
      entityConnectionTimeout = Measure.of(
          connectionTimeout.getExactQuantity().multiply(BigInteger.valueOf(3)),
          connectionTimeout.getUnit());
    }
  }
}
