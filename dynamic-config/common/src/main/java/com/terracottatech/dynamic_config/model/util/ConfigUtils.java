/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package com.terracottatech.dynamic_config.model.util;

import com.terracottatech.dynamic_config.model.Node;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.terracottatech.dynamic_config.model.util.ParameterSubstitutor.substitute;


public class ConfigUtils {

  public static Path createTempTcConfig(Node node, Path root) {
    try {
      Path temporaryTcConfigXml = Files.createTempFile(substitute(root), "tc-config-tmp.", ".xml");
      String defaultConfig = "<tc-config xmlns=\"http://www.terracotta.org/config\">\n" +
          "   <plugins>\n" +
          getDataDirectoryConfig(node, root) +
          getSecurityConfig(node, root) +
          "   </plugins>\n" +
          "    <servers>\n" +
          "        <server host=\"${HOSTNAME}\" name=\"${NAME}\" bind=\"${BIND}\">\n" +
          "            <logs>${LOGS}</logs>\n" +
          "            <tsa-port>${PORT}</tsa-port>\n" +
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
          .replace("${LOGS}", root.resolve(node.getNodeLogDir()).toString())
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

  private static String getSecurityConfig(Node node, Path root) {
    StringBuilder sb = new StringBuilder();

    if (node.getSecurityDir() != null) {
      sb.append("<service>");
      sb.append("<security xmlns=\"http://www.terracottatech.com/config/security\">");

      if (node.getSecurityAuditLogDir() != null) {
        sb.append("<audit-directory>").append(root.resolve(node.getSecurityAuditLogDir())).append("</audit-directory>");
      }

      sb.append("<security-root-directory>").append(root.resolve(node.getSecurityDir())).append("</security-root-directory>");

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

  private static String getDataDirectoryConfig(Node node, Path root) {
    String dataDirectoryConfig = "     <config xmlns:data=\"http://www.terracottatech.com/config/data-roots\">\n" +
        "     <data:data-directories>\n" +
        "         <data:directory name=\"data\" use-for-platform=\"true\">${DATA_DIR}</data:directory>\n" +
        "     </data:data-directories>\n" +
        "     </config>\n";

    return dataDirectoryConfig.replace("${DATA_DIR}", root.resolve(node.getNodeMetadataDir()).toString());
  }

  public static String generateNodeName() {
    UUID uuid = UUID.randomUUID();
    byte[] data = new byte[16];
    long msb = uuid.getMostSignificantBits();
    long lsb = uuid.getLeastSignificantBits();
    for (int i = 0; i < 8; i++) {
      data[i] = (byte) (msb & 0xff);
      msb >>>= 8;
    }
    for (int i = 8; i < 16; i++) {
      data[i] = (byte) (lsb & 0xff);
      lsb >>>= 8;
    }

    return "node-" + DatatypeConverter.printBase64Binary(data)
        // java-8 and other - compatible B64 url decoder use - and _ instead of + and /
        // padding can be ignored to shorten the UUID
        .replace('+', '-')
        .replace('/', '_')
        .replace("=", "");
  }
}
