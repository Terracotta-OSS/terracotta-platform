/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.inet.HostPort;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public class LogAction extends RemoteAction {

  private HostPort node;

  public void setNode(HostPort node) {
    this.node = node;
  }

  @Override
  public void run() {
    NomadChangeInfo[] logs = getChangeHistory(node);

    Arrays.sort(logs, Comparator.comparing(NomadChangeInfo::getVersion));
    Clock clock = Clock.systemDefaultZone();
    ZoneId zoneId = clock.getZone();
    DateTimeFormatter ISO_8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    Stream.of(logs)
        .map(log -> padCut(String.valueOf(log.getVersion()), 4)
            + " " + log.getCreationTimestamp().atZone(zoneId).toLocalDateTime().format(ISO_8601)
            + " " + log.getChangeUuid().toString()
            + " " + log.getChangeResultHash()
            + " " + log.getChangeRequestState().name()
            + " | " + log.getCreationUser()
            + "@" + log.getCreationHost()
            + " - " + log.getNomadChange().getSummary())
        .forEach(output::out);
  }

  private static String padCut(String s, int length) {
    StringBuilder sb = new StringBuilder(s);
    while (sb.length() < length) {
      sb.insert(0, " ");
    }
    return sb.substring(0, length);
  }
}
