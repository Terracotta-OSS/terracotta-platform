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
package org.terracotta.voter.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.time.Duration;
import java.util.List;

@Parameters
@Usage("(-connect-to <hostname:port>,<hostname:port>... -connect-to <hostname:port>,<hostname:port> ... | -vote-for <hostname:port>)) [-connect-timeout 30s] [-request-timeout 30s]")
public class OptionsParsingImpl implements OptionsParsing {

  @Parameter(names = {"-help", "-h"}, description = "Help", help = true)
  private boolean help;

  @Parameter(names = {"-vote-for", "-o"}, description = "Override vote to host:port")
  private String overrideHostPort;

  @Parameter(names = {"-connect-to", "-s"}, description = "Comma separated host:port to connect to (one per stripe)")
  private List<String> serversHostPort;

  @Parameter(names = {"-request-timeout", "-r", "--request-timeout"}, description = "Request timeout. Default: 10s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> requestTimeout = Measure.of(10, TimeUnit.SECONDS);

  @Parameter(names = {"-connect-timeout", "-connection-timeout", "-t", "--connection-timeout"}, description = "Connection timeout. Default: 10s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> connectionTimeout = Measure.of(10, TimeUnit.SECONDS);

  @Override
  public Options process() {
    validateOptions();
    Options options = new Options();
    options.setHelp(help);
    options.setOverrideHostPort(overrideHostPort);
    options.setServerHostPort(serversHostPort);
    options.setRequestTimeout(Duration.ofMillis(requestTimeout.getQuantity(TimeUnit.MILLISECONDS)));
    options.setConnectionTimeout(Duration.ofMillis(connectionTimeout.getQuantity(TimeUnit.MILLISECONDS)));
    return options;
  }

  private void validateOptions() {
    if (!help) {
      if (overrideHostPort == null && serversHostPort == null) {
        throw new RuntimeException("Neither the -vote-for option nor the regular -connect-to option provided");
      } else if (overrideHostPort != null && serversHostPort != null) {
        throw new RuntimeException("Either the -vote-for or the regular -connect-to option can be provided");
      }
    }
  }
}
