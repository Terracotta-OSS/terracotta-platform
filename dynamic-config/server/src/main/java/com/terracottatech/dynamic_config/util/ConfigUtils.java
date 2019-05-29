/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package com.terracottatech.dynamic_config.util;

import com.terracottatech.dynamic_config.Constants;
import com.terracottatech.dynamic_config.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.util.ParameterSubstitutor;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;


public class ConfigUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

  public static Path createTempTcConfig(Node node) {
    try {
      Path configPath = Files.createTempFile("tc-config", ".xml");

      String defaultConfig = "<tc-config xmlns=\"http://www.terracotta.org/config\">\n" +
          "   <plugins>\n" +
          getDataDirectoryConfig(node) +
          getSecurityConfig(node) +
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
          .replace("${LOGS}", node.getNodeLogDir().toString())
          .replace("${GROUP-BIND}", node.getNodeGroupBindAddress())
          .replace("${GROUP-PORT}", String.valueOf(node.getNodeGroupPort()))
          .replace("${RECONNECT_WINDOW}", String.valueOf((int)(node.getClientReconnectWindow().getType().toSeconds(node.getClientReconnectWindow().getQuantity()))));

      Files.write(configPath, configuration.getBytes(StandardCharsets.UTF_8));
      return configPath;
    } catch (IOException e) {
      throw new RuntimeException("Unable to create temp file for storing the configuration", e);
    }
  }

  private static String getSecurityConfig(Node node) {
    StringBuilder sb = new StringBuilder();

    if (node.getSecurityDir() != null) {
      sb.append("<service>");
      sb.append("<security xmlns=\"http://www.terracottatech.com/config/security\">");

      if (node.getSecurityAuditLogDir() != null) {
        sb.append("<audit-directory>").append(node.getSecurityAuditLogDir()).append("</audit-directory>");
      }

      sb.append("<security-root-directory>").append(node.getSecurityDir()).append("</security-root-directory>");

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

  private static String getDataDirectoryConfig(Node node) {
    String dataDirectoryConfig = "     <config xmlns:data=\"http://www.terracottatech.com/config/data-roots\">\n" +
                                 "     <data:data-directories>\n" +
                                 "         <data:directory name=\"data\" use-for-platform=\"true\">${DATA_DIR}</data:directory>\n" +
                                 "     </data:data-directories>\n" +
                                 "     </config>\n";

    return dataDirectoryConfig.replace("${DATA_DIR}", node.getNodeMetadataDir().toString());
  }

  public static Optional<String> findConfigRepo(String nodeConfigDir) {
    String substitutedConfigDir = getSubstitutedConfigDir(nodeConfigDir);
    Optional<String> found = Optional.empty();
    try (Stream<Path> stream = Files.list(Paths.get(substitutedConfigDir).resolve(Constants.NOMAD_CONFIG_DIR))) {
      found = stream.map(path -> path.getFileName().toString()).filter(fileName -> fileName.matches(Constants.CONFIG_REPO_FILENAME_REGEX)).findAny();
    } catch (IOException e) {
      LOGGER.debug("Reading cluster config repository from: {} resulted in exception: {}", substitutedConfigDir, e);
    }

    if (found.isPresent()) {
      LOGGER.info("Found cluster config repository at: {}", substitutedConfigDir);
    } else {
      LOGGER.info("Did not find cluster config repository in: {}", substitutedConfigDir);
    }
    return found;
  }

  public static String getSubstitutedConfigDir(String nodeConfigDir) {
    String specifiedOrDefaultConfigDir = nodeConfigDir == null ? Constants.DEFAULT_CONFIG_DIR : nodeConfigDir;
    return ParameterSubstitutor.substitute(specifiedOrDefaultConfigDir);
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
