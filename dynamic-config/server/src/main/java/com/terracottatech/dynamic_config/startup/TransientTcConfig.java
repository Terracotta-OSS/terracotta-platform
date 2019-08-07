/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.utilities.PathResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.terracottatech.dynamic_config.util.ParameterSubstitutor.substitute;

public class TransientTcConfig {
  private final Node node;
  private final PathResolver pathResolver;

  public TransientTcConfig(Node node, PathResolver pathResolver) {
    this.node = node;
    this.pathResolver = pathResolver;
  }

  public Path createTempTcConfigFile() {
    try {
      Path temporaryTcConfigXml = Files.createTempFile(substitute(pathResolver.getBaseDir()), "tc-config-tmp.", ".xml");
      String defaultConfig = "<tc-config xmlns=\"http://www.terracotta.org/config\">\n" +
          "    <plugins>\n" +
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
          .replace("${NAME}", node.getNodeName())
          .replace("${BIND}", node.getNodeBindAddress())
          .replace("${PORT}", String.valueOf(node.getNodePort()))
          .replace("${LOGS}", node.getNodeLogDir().toString())
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
        sb.append("<audit-directory>").append(substitute(pathResolver.resolve(node.getSecurityAuditLogDir()))).append("</audit-directory>");
      }

      sb.append("<security-root-directory>").append(substitute(pathResolver.resolve(node.getSecurityDir()))).append("</security-root-directory>");

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

    return dataDirectoryConfig.replace("${DATA_DIR}", substitute(pathResolver.resolve(node.getNodeMetadataDir())).toString());
  }
}
