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
package org.terracotta.dynamic_config.server.startup;

import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.PathResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.terracotta.dynamic_config.server.startup.XmlUtils.escapeXml;
import static java.lang.System.lineSeparator;

public class TransientTcConfig {
  private final Node node;
  private final PathResolver pathResolver;
  private final IParameterSubstitutor parameterSubstitutor;

  public TransientTcConfig(Node node, PathResolver pathResolver, IParameterSubstitutor parameterSubstitutor) {
    this.node = node;
    this.pathResolver = pathResolver;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  public Path createTempTcConfigFile() {
    try {
      Path substituted = parameterSubstitutor.substitute(pathResolver.getBaseDir());
      Path temporaryTcConfigXml = Files.createTempFile(substituted, "tc-config-tmp.", ".xml");
      String defaultConfig = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + lineSeparator() +
          "    <plugins>" + lineSeparator() +
          getOffHeapConfig() +
          getDataDirectoryConfig() +
          getSecurityConfig() +
          "    </plugins>" + lineSeparator() +
          "    <servers>" + lineSeparator() +
          "        <server host=\"${HOSTNAME}\" name=\"${NAME}\" bind=\"${BIND}\">" + lineSeparator() +
          "            <logs>${LOGS}</logs>" + lineSeparator() +
          "            <tsa-port bind=\"${BIND}\">${PORT}</tsa-port>" + lineSeparator() +
          "            <tsa-group-port bind=\"${GROUP-BIND}\">${GROUP-PORT}</tsa-group-port>" + lineSeparator() +
          "        </server>" + lineSeparator() +
          "        <client-reconnect-window>${RECONNECT_WINDOW}</client-reconnect-window>" + lineSeparator() +
          "    </servers>" + lineSeparator() +
          "</tc-config>";

      String configuration = defaultConfig
          .replace("${HOSTNAME}", node.getNodeHostname())
          .replace("${NAME}", escapeXml(node.getNodeName()))
          .replace("${BIND}", node.getNodeBindAddress())
          .replace("${PORT}", String.valueOf(node.getNodePort()))
          .replace("${LOGS}", escapeXml(node.getNodeLogDir().toString()))
          .replace("${GROUP-BIND}", node.getNodeGroupBindAddress())
          .replace("${GROUP-PORT}", String.valueOf(node.getNodeGroupPort()))
          .replace("${RECONNECT_WINDOW}", String.valueOf((int) (node.getClientReconnectWindow().getUnit().toSeconds(node.getClientReconnectWindow().getQuantity()))));

      Files.write(temporaryTcConfigXml, configuration.getBytes(StandardCharsets.UTF_8));
      temporaryTcConfigXml.toFile().deleteOnExit();
      return temporaryTcConfigXml;
    } catch (IOException e) {
      throw new RuntimeException("Unable to create temp file for storing the configuration", e);
    }
  }

  private String getSecurityConfig() {
    StringBuilder sb = new StringBuilder();

    if (node.getSecurityDir() != null) {
      sb.append("<service>");
      sb.append("<security xmlns=\"http://www.terracottatech.com/config/security\">");

      if (node.getSecurityAuditLogDir() != null) {
        sb.append("<audit-directory>")
            .append(escapeXml(parameterSubstitutor.substitute(pathResolver.resolve(node.getSecurityAuditLogDir()).toString())))
            .append("</audit-directory>");
      }

      sb.append("<security-root-directory>")
          .append(escapeXml(parameterSubstitutor.substitute(pathResolver.resolve(node.getSecurityDir())).toString()))
          .append("</security-root-directory>");

      if (node.isSecuritySslTls()) {
        sb.append("<ssl-tls/>");
      }

      if (node.getSecurityAuthc() != null) {
        sb.append("<authentication>");
        sb.append("<").append(node.getSecurityAuthc()).append("/>");
        sb.append("</authentication>");
      }

      if (node.isSecurityWhitelist()) {
        sb.append("<whitelist/>");
      }

      sb.append("</security>");
      sb.append("</service>");
    }

    return sb.toString();
  }

  private String getDataDirectoryConfig() {
    if (node.getNodeMetadataDir() == null) {
      return "";
    }
    String dataDirectoryConfig = "    <config xmlns:data=\"http://www.terracottatech.com/config/data-roots\">" + lineSeparator() +
        "    <data:data-directories>" + lineSeparator() +
        "        <data:directory name=\"data\" use-for-platform=\"true\">${DATA_DIR}</data:directory>" + lineSeparator() +
        "    </data:data-directories>" + lineSeparator() +
        "    </config>" + lineSeparator();

    return dataDirectoryConfig.replace("${DATA_DIR}", escapeXml(parameterSubstitutor.substitute(pathResolver.resolve(node.getNodeMetadataDir())).toString()));
  }

  private String getOffHeapConfig() {
    String prefix = "    <config xmlns:ohr=\"http://www.terracotta.org/config/offheap-resource\">" + lineSeparator() +
        "      <ohr:offheap-resources>" + lineSeparator();

    String middle = node.getOffheapResources().entrySet()
        .stream()
        .map(entry -> "<ohr:resource name=\"" + escapeXml(entry.getKey()) + "\" unit=\"" + entry.getValue().getUnit().getShortName() + "\">" + entry.getValue().getQuantity(entry.getValue().getUnit()) + "</ohr:resource>")
        .collect(Collectors.joining(lineSeparator()));

    String suffix =
        "      </ohr:offheap-resources>" + lineSeparator() +
            "    </config>" + lineSeparator();

    return prefix + middle + suffix;
  }
}
