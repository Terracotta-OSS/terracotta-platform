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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.nomad.server.NomadChangeInfo;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "log", commandDescription = "Log all the configuration changes of a node and their details")
@Usage("log -s <hostname[:port]>")
public class LogCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", converter = InetSocketAddressConverter.class, required = true)
  private InetSocketAddress node;

  @Override
  public void validate() {
    validateAddress(node);
  }

  @Override
  public void run() {
    logger.info("Configuration logs from {}:", node);

    NomadChangeInfo[] logs = getLogs();

    Arrays.sort(logs, Comparator.comparing(NomadChangeInfo::getVersion));
    Clock clock = Clock.systemDefaultZone();
    ZoneId zoneId = clock.getZone();
    DateTimeFormatter ISO_8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    String formattedChanges = Stream.of(logs)
        .map(log -> padCut(String.valueOf(log.getVersion()), 4)
            + " " + log.getCreationTimestamp().atZone(zoneId).toLocalDateTime().format(ISO_8601)
            + " " + log.getChangeUuid().toString()
            + " " + log.getChangeRequestState().name()
            + " | " + log.getCreationUser()
            + "@" + log.getCreationHost()
            + " - " + log.getNomadChange().getSummary())
        .collect(joining(lineSeparator()));

    if (formattedChanges.isEmpty()) {
      formattedChanges = "<empty>";
    }

    logger.info("{}{}{}", lineSeparator(), formattedChanges, lineSeparator());
  }

  private NomadChangeInfo[] getLogs() {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node)) {
      return diagnosticService.getProxy(TopologyService.class).getChangeHistory();
    }
  }

  private static String padCut(String s, int length) {
    StringBuilder sb = new StringBuilder(s);
    while (sb.length() < length) {
      sb.insert(0, " ");
    }
    return sb.substring(0, length);
  }
}
