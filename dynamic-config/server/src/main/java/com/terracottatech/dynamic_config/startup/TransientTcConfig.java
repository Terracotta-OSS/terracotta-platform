/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.PathResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.terracottatech.utilities.XmlUtils.escapeXml;

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
      String defaultConfig = "<tc-config xmlns=\"http://www.terracotta.org/config\">\n" +
          "    <plugins>\n" +
          getOffHeapConfig() +
          getDataDirectoryConfig() +
          getSecurityConfig() +
          "    </plugins>\n" +
          "    <servers>\n" +
          "        <server host=\"${HOSTNAME}\" name=\"${NAME}\" bind=\"${BIND}\">\n" +
          "            <logs>${LOGS}</logs>\n" +
          "            <tsa-port bind=\"${BIND}\">${PORT}</tsa-port>\n" +
          "            <tsa-group-port bind=\"${GROUP-BIND}\">${GROUP-PORT}</tsa-group-port>\n" +
          "        </server>\n" +
          "        <client-reconnect-window>${RECONNECT_WINDOW}</client-reconnect-window>\n" +
          "    </servers>\n" +
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
    String dataDirectoryConfig = "    <config xmlns:data=\"http://www.terracottatech.com/config/data-roots\">\n" +
        "    <data:data-directories>\n" +
        "        <data:directory name=\"data\" use-for-platform=\"true\">${DATA_DIR}</data:directory>\n" +
        "    </data:data-directories>\n" +
        "    </config>\n";

    return dataDirectoryConfig.replace("${DATA_DIR}", escapeXml(parameterSubstitutor.substitute(pathResolver.resolve(node.getNodeMetadataDir())).toString()));
  }

  private String getOffHeapConfig() {
    String prefix = "    <config xmlns:ohr=\"http://www.terracotta.org/config/offheap-resource\">\n" +
        "      <ohr:offheap-resources>\n";

    String middle = node.getOffheapResources().entrySet()
        .stream()
        .map(entry -> "<ohr:resource name=\"" + escapeXml(entry.getKey()) + "\" unit=\"" + entry.getValue().getUnit().getShortName() + "\">" + entry.getValue().getQuantity(entry.getValue().getUnit()) + "</ohr:resource>")
        .collect(Collectors.joining("\n"));

    String suffix =
        "      </ohr:offheap-resources>\n" +
            "    </config>\n";

    return prefix + middle + suffix;
  }
}
