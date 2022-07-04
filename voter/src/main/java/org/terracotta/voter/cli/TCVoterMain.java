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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.voter.ActiveVoter;
import org.terracotta.voter.TCVoter;
import org.terracotta.voter.TCVoterImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TCVoterMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterMain.class);

  private static final String ID = UUID.randomUUID().toString();

  public static void main(String[] args) {
    TCVoterMain main = new TCVoterMain();
    writePID();
    main.processArgs(args);
  }

  public void processArgs(String[] args) {
    OptionsParsing optionsParsing = getParsingObject();
    CustomJCommander jCommander = new CustomJCommander(optionsParsing);
    jCommander.parse(args);
    Options options = optionsParsing.process();

    if (options.isHelp()) {
      jCommander.usage();
      return;
    }

    Optional<Properties> connectionProps = getConnectionProperties(options);
    if (options.getServersHostPort() != null) {
      processServerArg(connectionProps, options.getServersHostPort().toArray(new String[0]));
    } else if (options.getOverrideHostPort() != null) {
      String hostPort = options.getOverrideHostPort();
      validateHostPort(hostPort);
      getVoter(connectionProps).overrideVote(hostPort);
    } else {
      throw new AssertionError("This should not happen");
    }
  }

  protected OptionsParsing getParsingObject() {
    return new OptionsParsingImpl();
  }

  protected Optional<Properties> getConnectionProperties(Options option) {
    return Optional.empty();
  }

  protected void processServerArg(Optional<Properties> connectionProps, String[] stripes) {
    validateStripesLimit(stripes);
    String[] hostPorts = stripes[0].split(",");
    for (String hostPort : hostPorts) {
      validateHostPort(hostPort);
    }
    startVoter(connectionProps, hostPorts);
  }

  protected TCVoter getVoter(Optional<Properties> connectionProps) {
    return new TCVoterImpl();
  }

  protected void startVoter(Optional<Properties> connectionProps, String... hostPorts) {
    new ActiveVoter(ID, new CompletableFuture<>(), connectionProps, hostPorts).start();
  }

  protected void validateStripesLimit(String[] args) {
    if (args.length > 1) {
      throw new RuntimeException("Usage of multiple -connect-to options not supported");
    }
  }

  protected void validateHostPort(String hostPort) {
    URI uri;
    try {
      uri = new URI("tc://" + hostPort);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    if (uri.getHost() == null || uri.getPort() == -1) {
      throw new RuntimeException("Invalid host:port combination provided: " + hostPort);
    }
  }

  protected static void writePID() {
    try {
      String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      long pid = Long.parseLong(processName.split("@")[0]);
      LOGGER.info("PID is {}", pid);
    } catch (Throwable t) {
      LOGGER.warn("Unable to fetch the PID of this process.");
    }
  }
}
