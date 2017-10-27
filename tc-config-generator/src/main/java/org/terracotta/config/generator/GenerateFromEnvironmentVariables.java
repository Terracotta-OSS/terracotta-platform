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
package org.terracotta.config.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.config.service.ServiceConfigParser;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;


public class GenerateFromEnvironmentVariables {

  private static final Logger LOGGER = LoggerFactory.getLogger(GenerateFromEnvironmentVariables.class);

  public static final String CLIENT_RECONNECT_WINDOW = "CLIENT_RECONNECT_WINDOW";
  public static final String PLATFORM_PERSISTENCE = "PLATFORM_PERSISTENCE";
  public static final String OFFHEAP_UNIT = "OFFHEAP_UNIT";
  public static final String LEASE_LENGTH = "LEASE_LENGTH";
  public static final String TC_SERVER = "TC_SERVER";
  public static final String DATA_DIRECTORY = "DATA_DIRECTORY";

  public static final String OFFHEAP_NAMESPACE = "http://www.terracotta.org/config/offheap-resource";
  public static final String LEASE_NAMESPACE = "http://www.terracotta.org/service/lease";
  public static final String BACKUP_NAMESPACE = "http://www.terracottatech.com/config/backup-restore";
  public static final String DATAROOTS_NAMESPACE = "http://www.terracottatech.com/config/data-roots";

  private List<String> supportedNamespacesPaths = new ArrayList<>();

  public GenerateFromEnvironmentVariables(List<String> supportedConfigurations) {
    this.supportedNamespacesPaths = supportedConfigurations;
  }

  public static void main(String[] args) {

    List<String> supportedConfigurations = detectSupportedConfigurations();
    LOGGER.info("This Terracotta Server distribution supports those optional configuration namespaces : \n{}", supportedConfigurations);

    Map<String, String> envMap = System.getenv();
    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    ConfigurationModel configurationModel = generateFromEnvironmentVariables.createModelFromMapVariables(envMap);
    generateFromEnvironmentVariables.generateXml(configurationModel, "./");
  }

  static List<String> detectSupportedConfigurations() {
    List<String> supportedNamespacesPaths = new ArrayList<>();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    for (ServiceConfigParser parser : loadServiceConfigurationParserClasses(loader)) {
      supportedNamespacesPaths.add(parser.getNamespace().toString());
    }
    for (ExtendedConfigParser parser : loadConfigurationParserClasses(loader)) {
      supportedNamespacesPaths.add(parser.getNamespace().toString());
    }
    return supportedNamespacesPaths;
  }

  ConfigurationModel createModelFromMapVariables(Map<String, String> envMap) throws IllegalArgumentException {
    ConfigurationModel configurationModel = new ConfigurationModel();

    if(supportedNamespacesPaths.contains(DATAROOTS_NAMESPACE)) {
      retrieveAndSetPlatformPersistence(configurationModel, envMap);
      configurationModel.getDataDirectories().addAll(retrieveNamesWithPrefix(envMap, DATA_DIRECTORY));
    }
    if(supportedNamespacesPaths.contains(BACKUP_NAMESPACE)) {
      configurationModel.setBackups(true);
    }
    if(supportedNamespacesPaths.contains(LEASE_NAMESPACE)) {
      retrieveAndSetLeaseLength(configurationModel, envMap);
    }
    if(supportedNamespacesPaths.contains(OFFHEAP_NAMESPACE)) {
      retrieveAndSetOffheapUnit(configurationModel, envMap);
      retrieveAndSetOffHeapResources(configurationModel, envMap);
    }

    retrieveAndSetClientReconnectWindow(configurationModel, envMap);
    configurationModel.getServerNames().addAll(retrieveNamesWithPrefix(envMap, TC_SERVER));
    return configurationModel;

  }

  private List<String> retrieveNamesWithPrefix(Map<String, String> envMap, String envVariablePrefix) {
    Map<Integer, String> integerStringMap = new TreeMap<>();
    envMap.entrySet().forEach(envEntry -> {
      if (envEntry.getKey().startsWith(envVariablePrefix)) {
        String regexp = "(" + envVariablePrefix + ")(\\d+)";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(envEntry.getKey());
        if (matcher.matches()) {
          int key = Integer.parseInt(matcher.group(2));
          String serverName = envEntry.getValue();
          integerStringMap.put(key, serverName);
        }
      }
    });

    if (!Arrays.equals(
        integerStringMap.keySet().toArray(new Integer[]{}),
        IntStream.rangeClosed(1, integerStringMap.keySet().size()).boxed().toArray())
        ) {
      throw new IllegalArgumentException(envVariablePrefix + " environment variables are not numbered with the following pattern : " + envVariablePrefix + "1, " + envVariablePrefix + "2, etc.");
    }
    return new ArrayList<>(integerStringMap.values());
  }

  private void retrieveAndSetOffHeapResources(ConfigurationModel ConfigurationModel, Map<String, String> envMap) {
    Map<Integer, Map.Entry<String, Integer>> integerStringMap = new TreeMap<>();
    envMap.entrySet().forEach(envEntry -> {
      if (envEntry.getKey().startsWith("OFFHEAP_RESOURCE_NAME")) {
        String regexp = "(OFFHEAP_RESOURCE_NAME)(\\d+)";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(envEntry.getKey());
        if (matcher.matches()) {
          int key = Integer.parseInt(matcher.group(2));
          String offHeapResourceName = envEntry.getValue();
          try {
            Integer offHeapResourceSize = Integer.parseInt(envMap.get("OFFHEAP_RESOURCE_SIZE" + key));
            if (offHeapResourceSize > 0) {
              Map.Entry<String, Integer> integerEntry = new AbstractMap.SimpleEntry<>(offHeapResourceName, offHeapResourceSize);
              integerStringMap.put(key, integerEntry);
            } else {
              throw new IllegalArgumentException("Value for : " + "OFFHEAP_RESOURCE_SIZE" + key + " should be > 0");

            }
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value for : " + "OFFHEAP_RESOURCE_SIZE" + key + " should be an integer");
          }
        }
      }
    });

    if (!Arrays.equals(
        integerStringMap.keySet().toArray(new Integer[]{}),
        IntStream.rangeClosed(1, integerStringMap.keySet().size()).boxed().toArray())
        ) {
      throw new IllegalArgumentException("OFFHEAP_RESOURCE_NAME environment variables are not numbered with the following pattern : OFFHEAP_RESOURCE_NAME1, OFFHEAP_RESOURCE_NAME2, etc.");
    }
    ConfigurationModel.getOffheapResources().addAll(new ArrayList<>(integerStringMap.values()));
  }

  private void retrieveAndSetOffheapUnit(ConfigurationModel ConfigurationModel, Map<String, String> envMap) {
    String offHeapUnit = envMap.get(OFFHEAP_UNIT);
    if (offHeapUnit != null && !offHeapUnit.trim().equals("")) {
      ConfigurationModel.setOffheapUnit(offHeapUnit);
    } else {
      throw new IllegalArgumentException(OFFHEAP_UNIT + " was not set but is a mandatory variable");
    }
  }


  private void retrieveAndSetLeaseLength(ConfigurationModel ConfigurationModel, Map<String, String> envMap) {
    String leaseLength = envMap.get(LEASE_LENGTH);
    try {
      ConfigurationModel.setLeaseLength(Integer.parseInt(leaseLength));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Value for : " + LEASE_LENGTH + " should be an integer");
    }
  }

  private void retrieveAndSetClientReconnectWindow(ConfigurationModel ConfigurationModel, Map<String, String> envMap) {
    String clientReconnectWindow = envMap.get(CLIENT_RECONNECT_WINDOW);
    if (clientReconnectWindow == null) {
      throw new IllegalArgumentException("Missing value for : " + CLIENT_RECONNECT_WINDOW);
    }
    try {
      ConfigurationModel.setClientReconnectWindow(Integer.parseInt(clientReconnectWindow));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Value for : " + CLIENT_RECONNECT_WINDOW + " should be an integer");
    }
  }

  private void retrieveAndSetPlatformPersistence(ConfigurationModel ConfigurationModel, Map<String, String> envMap) {
    boolean platformPersistence = Boolean.parseBoolean(envMap.get(PLATFORM_PERSISTENCE));
    ConfigurationModel.setPlatformPersistence(platformPersistence);
  }

  File generateXml(ConfigurationModel configurationModel, String directoryPath) {
    // starts xml
    StringBuilder sb = new StringBuilder(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<tc-config xmlns=\"http://www.terracotta.org/config\">\n");
    // plugins
    sb.append(
        "  <plugins>\n" +
            "\n");

    // connection leasing
    if (configurationModel.getLeaseLength() > -1) {
      sb.append(
          "    <service>\n" +
              "      <lease:connection-leasing xmlns:lease=\"http://www.terracotta.org/service/lease\">\n" +
              "        <lease:lease-length unit=\"seconds\">" + configurationModel.getLeaseLength() + "</lease:lease-length>\n" +
              "      </lease:connection-leasing>\n" +
              "    </service>\n\n");
    }

    // offheaps
    if (configurationModel.getOffheapResources().size() > 0) {
      sb.append("" +
          "    <config>\n" +
          "      <ohr:offheap-resources xmlns:ohr=\"http://www.terracotta.org/config/offheap-resource\">\n");
      for (int r = 0; r < configurationModel.getOffheapResources().size(); r++) {
        String name = configurationModel.getOffheapResources().get(r).getKey();
        Integer size = configurationModel.getOffheapResources().get(r).getValue();
        sb.append(
            "        <ohr:resource name=\"" + name + "\" unit=\"" + configurationModel.getOffheapUnit() + "\">" + size + "</ohr:resource>\n");
      }
      sb.append("" +
          "      </ohr:offheap-resources>\n" +
          "    </config>\n" +
          "\n");
    }

    if (configurationModel.isPlatformPersistence() || configurationModel.getDataDirectories().size() > 0) {
      // dataroots
      sb.append(
          "    <config>\n" +
              "      <data:data-directories xmlns:data=\"http://www.terracottatech.com/config/data-roots\">\n");
      // platform persistence
      if (configurationModel.isPlatformPersistence()) {
        sb.append("        <data:directory name=\"platform\" use-for-platform=\"true\">/data/data-directories/platform</data:directory>\n");
      }


      for (int i = 0; i < configurationModel.getDataDirectories().size(); i++) {
        sb.append("        <data:directory name=\"" + configurationModel.getDataDirectories().get(i) + "\" use-for-platform=\"false\">/data/data-directories/" + configurationModel.getDataDirectories().get(i) + "</data:directory>\n");
      }

      // end data roots
      sb.append(
          "      </data:data-directories>\n" +
              "    </config>\n" +
              "\n");

      // backup
      if (configurationModel.isBackups())
        sb.append(
            "    <service>\n" +
                "      <backup:backup-restore xmlns:backup=\"http://www.terracottatech.com/config/backup-restore\">\n" +
                "        <backup:backup-location path=\"/data/backups/\"/>\n" +
                "      </backup:backup-restore>\n" +
                "    </service>\n" +
                "\n");
    }

    // end plugins
    sb.append(
        "  </plugins>\n");

    // servers
    sb.append(
        "\n" +
            "  <servers>\n" +
            "\n");

    for (String hostname : configurationModel.getServerNames()) {
      sb.append("" +
          "    <server host=\"" + hostname + "\" name=\"" + hostname + "\">\n" +
          "      <tsa-port>9410</tsa-port>\n" +
          "      <tsa-group-port>9430</tsa-group-port>\n" +
          "    </server>\n\n");
    }

    // reconnect window
    if(configurationModel.getClientReconnectWindow() > -1) {
      sb.append(
          "    <client-reconnect-window>" + configurationModel.getClientReconnectWindow() + "</client-reconnect-window>\n\n");
    }
    // ends servers
    sb.append(
        "  </servers>\n\n");
    sb.append(
        "</tc-config>");

    String xml = sb.toString();


    String filename = "tc-config-generated.xml";
    File location = new File(directoryPath, filename);
    try {
      Files.write(location.toPath(), xml.getBytes("UTF-8"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return location;

  }

  private static ServiceLoader<ServiceConfigParser> loadServiceConfigurationParserClasses(ClassLoader loader) {
    return ServiceLoader.load(ServiceConfigParser.class, loader);
  }

  private static ServiceLoader<ExtendedConfigParser> loadConfigurationParserClasses(ClassLoader loader) {
    return ServiceLoader.load(ExtendedConfigParser.class, loader);
  }

}
