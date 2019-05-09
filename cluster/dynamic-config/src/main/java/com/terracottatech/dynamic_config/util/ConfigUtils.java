/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package com.terracottatech.dynamic_config.util;

import com.terracottatech.dynamic_config.Constants;
import com.terracottatech.dynamic_config.config.Node;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.Constants.CONFIG_REPO_FILENAME_REGEX;
import static com.terracottatech.dynamic_config.Constants.NOMAD_CONFIG_DIR;


public class ConfigUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

  public static Path createTempTcConfig(Node node) {
    try {
      Path configPath = Files.createTempFile("tc-config", ".xml");

      String defaultConfig = "<tc-config xmlns=\"http://www.terracotta.org/config\">\n" +
          "   <plugins>\n" +
          getDataDirectoryConfig(node) +
          getOffheapResourcesConfig(node) +
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

      String configuration = defaultConfig.replaceAll(Pattern.quote("${HOSTNAME}"), node.getNodeHostname())
          .replaceAll(Pattern.quote("${NAME}"), node.getNodeName())
          .replaceAll(Pattern.quote("${BIND}"), node.getNodeBindAddress())
          .replaceAll(Pattern.quote("${PORT}"), String.valueOf(node.getNodePort()))
          .replaceAll(Pattern.quote("${LOGS}"), node.getNodeLogDir().toString())
          .replaceAll(Pattern.quote("${GROUP-BIND}"), node.getNodeGroupBindAddress())
          .replaceAll(Pattern.quote("${GROUP-PORT}"), String.valueOf(node.getNodeGroupPort()))
          .replaceAll(Pattern.quote("${RECONNECT_WINDOW}"), String.valueOf(node.getClientReconnectWindow()));

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

    return dataDirectoryConfig.replaceAll(Pattern.quote("${DATA_DIR}"), node.getNodeMetadataDir().toString());
  }

  private static String getOffheapResourcesConfig(Node node) {
    String configPrefix = "     <config xmlns:ofr=\"http://www.terracotta.org/config/offheap-resource\">\n" +
        "     <ofr:offheap-resources>\n";
    String configSuffix = "     </ofr:offheap-resources>\n" +
        "     </config>\n";

    StringBuilder sb = new StringBuilder();
    String dataDirectoryConfig = "         <ofr:resource name=\"${NAME}\" unit=\"B\">${QUANTITY}</ofr:resource>\n";
    node.getOffheapResources().forEach((key, value) -> {
      String substituted = dataDirectoryConfig
          .replaceAll(Pattern.quote("${NAME}"), key)
          .replaceAll(Pattern.quote("${QUANTITY}"), String.valueOf(value));
      sb.append(substituted);
    });
    return configPrefix + sb.toString() + configSuffix;
  }

  public static Optional<String> findConfigRepo(String nodeConfigDir) {
    String specifiedOrDefaultConfigDir = nodeConfigDir == null ? Constants.DEFAULT_CONFIG_DIR : nodeConfigDir;
    String substitutedConfigDir = ParameterSubstitutor.substitute(specifiedOrDefaultConfigDir);

    try (Stream<Path> stream = Files.list(Paths.get(substitutedConfigDir).resolve(NOMAD_CONFIG_DIR))) {
      return stream.map(path -> path.getFileName().toString()).filter(fileName -> fileName.matches(CONFIG_REPO_FILENAME_REGEX)).findAny();
    } catch (IOException e) {
      LOGGER.debug("Reading cluster config repository from: {} resulted in exception: {}", substitutedConfigDir, e);
    }
    return Optional.empty();
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
