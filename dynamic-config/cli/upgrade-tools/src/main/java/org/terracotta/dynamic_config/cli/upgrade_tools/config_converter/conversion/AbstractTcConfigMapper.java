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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.NameGenerator;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ConfigConversionException;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorParamKey;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.InvalidInputConfigurationContentException;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.InvalidInputException;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.validators.ValidationWrapper;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.NonSubstitutingTCConfigurationParser;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.TcConfigMapper;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.XmlUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.terracotta.common.struct.Tuple2.tuple2;

public abstract class AbstractTcConfigMapper implements TcConfigMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTcConfigMapper.class);

  private static String NS_DATA_ROOT = "http://www.terracottatech.com/config/data-roots";
  private static String NS_PLATFORM_PERSISTENCE = "http://www.terracottatech.com/config/platform-persistence";

  static final String TERRACOTTA_CONFIG_NAMESPACE = "http://www.terracotta.org/config";
  static final String NAME_NODE_NAME = "name";
  static final String SERVERS_NODE_NAME = "servers";
  static final String SERVER_NODE_NAME = "server";

  static final String PLUGINS_NODE_NAME = "plugins";
  static final String PLATFORM_PERSISTENCE_DATA_DIR_ID_ATTR_NAME = "data-directory-id";
  static final String DATA_DIR_USER_FOR_PLATFORM_ATTR_NAME = "use-for-platform";
  static final String NAME_ATTR_NAME = "name";

  private final Map<Path, Node> configFileRootNodeMap = new HashMap<>();
  private final List<String> allServers = new ArrayList<>();

  protected ClassLoader classLoader;

  /**
   * Returns a cluster formed of 1 stripe N nodes representing an old tc-config XML file
   */
  public abstract Cluster getStripe(String xml);

  @Override
  public void init(ClassLoader classLoader) {
    this.classLoader = requireNonNull(classLoader);
  }

  @Override
  public Cluster parseConfig(String clusterName, List<String> stripeNames, Path... tcConfigPaths) {
    Map<Integer, Path> configFilePerStripeMap = new HashMap<>();
    Map<Tuple2<Integer, String>, Node> stripeServerConfigNodeMap = new HashMap<>();
    int stripeId = 1;
    for (Path tcConfigPath : tcConfigPaths) {
      configFilePerStripeMap.put(stripeId++, tcConfigPath);
    }

    for (Map.Entry<Integer, Path> stringPathEntry : configFilePerStripeMap.entrySet()) {
      createServerConfigMapFunction(stripeServerConfigNodeMap, stringPathEntry.getKey(), stringPathEntry.getValue());
    }
    LOGGER.trace("Checking if deprecated platform-persistence needs to be converted to data-directory. Will convert if found.");
    handlePlatformPersistence(configFileRootNodeMap);
    LOGGER.trace("Validating contents of the configuration files");
    valueValidators();
    LOGGER.trace("Building Cluster");
    return getCluster(clusterName, stripeServerConfigNodeMap, stripeNames);
  }

  protected void valueValidators() {
    /*
    Creates a Map of configuration files and Map of namespace, corresponding plugin configuration
     */
    Map<Path, Map<String, Node>> configAndServiceNodesPerConfigFile = buildConfigurationFilePluginNodeMap();

    /*
    Validates all files have same number and type of plugin configuration (e.g.. one off-heap, one backup)
     */
    Set<String> namespaces = validateAllConfigurationFilesHaveSamePluginTypes(configAndServiceNodesPerConfigFile);
    /*
    Validates values inside each plugins are same/equivalent (e.g. all the configuration files have same number of
    off-heap resources and off-heap resource values are same in each configuration)
     */
    List<Tuple2<Map<Path, Node>, ValidationWrapper>> validatorsWithParams =
        prepareValidatorsForPluginConfigurations(namespaces, configAndServiceNodesPerConfigFile);

    validatePluginConfigurations(validatorsWithParams);
  }

  protected Set<String> validateAllConfigurationFilesHaveSamePluginTypes(Map<Path, Map<String, Node>> configAndServiceNodesPerConfigFile) {
    AtomicReference<Set<String>> previousSetReference = new AtomicReference<>();
    AtomicReference<Path> previousPath = new AtomicReference<>();
    configAndServiceNodesPerConfigFile.forEach((path, nodeMap) -> {
      if (previousSetReference.get() == null) {
        previousSetReference.set(nodeMap.keySet());
        previousPath.set(path);
        return;
      }
      Set<String> currentSet = nodeMap.keySet();
      if (!previousSetReference.get().equals(currentSet)) {
        throw new InvalidInputConfigurationContentException(
            ErrorCode.MISMATCHED_SERVICE_CONFIGURATION,
            "Mismatched Service Configuration",
            tuple2(ErrorParamKey.CONFIG_FILE.name(), path.toString()),
            tuple2(ErrorParamKey.CONFIG_FILE.name(), previousPath.get().toString())
        );
      }
      previousSetReference.set(nodeMap.keySet());
    });
    return previousSetReference.get();
  }

  protected List<Tuple2<Map<Path, Node>, ValidationWrapper>> prepareValidatorsForPluginConfigurations(Set<String> namespaces
      , Map<Path, Map<String, Node>> configAndServiceNodesPerConfigFile) {
    List<Tuple2<Map<Path, Node>, ValidationWrapper>> validatorsWithParams = new ArrayList<>();
    namespaces.forEach(namespace -> {
      Map<Path, Node> configFilesAndNodesMap = new HashMap<>();
      configAndServiceNodesPerConfigFile.forEach((path, nodeMap) -> {
        // For a give namespace, create a map of config-file -> plugin-configuration node present in that config-file
        Node node = nodeMap.get(namespace);
        configFilesAndNodesMap.put(path, node);
      });
      Supplier<ValidationWrapper> validatorSupplier = getValidatorSupplier(namespace);
      validatorsWithParams.add(tuple2(configFilesAndNodesMap, validatorSupplier.get()));
    });
    return validatorsWithParams;
  }

  protected void validatePluginConfigurations(List<Tuple2<Map<Path, Node>, ValidationWrapper>> validatorsWithParams) {
    validatorsWithParams.forEach(pair -> pair.getT2().check(pair.getT1()));
  }

  protected Node getRootNode(Path configFilePath) throws Exception {
    File configFile = configFilePath.toFile();
    try (FileInputStream in = new FileInputStream(configFile)) {
      return NonSubstitutingTCConfigurationParser.getRootElement(in, classLoader);
    }
  }

  protected Node getClonedParentDocNode(Node rootNode) {
    return XmlUtility.getClonedParentDocFromRootNode(rootNode);
  }

  protected List<String> extractServerNames(Node rootConfigNode) {
    List<String> serverNames = new ArrayList<>();
    for (int i = 0; i < rootConfigNode.getChildNodes().getLength(); i++) {
      Node childNode = rootConfigNode.getChildNodes().item(i);
      if (TERRACOTTA_CONFIG_NAMESPACE.equals(childNode.getNamespaceURI()) && SERVERS_NODE_NAME.equals(childNode.getLocalName())) {
        for (int j = 0; j < childNode.getChildNodes().getLength(); j++) {
          Node potentialServerNode = childNode.getChildNodes().item(j);
          if (TERRACOTTA_CONFIG_NAMESPACE.equals(potentialServerNode.getNamespaceURI())
              && SERVER_NODE_NAME.equals(potentialServerNode.getLocalName())) {
            String serverName = getAttributeValue(potentialServerNode, NAME_NODE_NAME);
            serverNames.add(serverName);
          }
        }
      }
    }
    return serverNames;
  }

  protected String getAttributeValue(Node node, String attributeName, boolean throwException) {
    Optional<String> attributeValue = getOptionalAttributeValue(node, attributeName);
    if (attributeValue.isPresent()) {
      return attributeValue.get();
    }
    if (throwException) {
      throw new ConfigConversionException(
          ErrorCode.INVALID_ATTRIBUTE_NAME,
          "Attribute " + attributeName + " is missing in Node " + node.getLocalName(),
          tuple2("AttributeName", attributeName),
          tuple2("nodeName", node.getLocalName())
      );
    }
    return null;
  }

  protected String getAttributeValue(Node node, String attributeName) {
    return getAttributeValue(node, attributeName, true);
  }

  protected void setAttributeValue(Node node, String attributeName, String attributeValue) {
    XmlUtility.setAttribute(node, attributeName, attributeValue);
  }

  protected void removeNode(Node node, boolean removeEmptyParent) {
    XmlUtility.removeNode(node, removeEmptyParent);
  }

  protected Optional<String> getOptionalAttributeValue(Node node, String attributeName) {
    return XmlUtility.getAttributeValue(node, attributeName);
  }

  protected Map<Path, Map<String, Node>> buildConfigurationFilePluginNodeMap() {
    Map<Path, Map<String, Node>> configAndServiceNodesPerConfigFile = new HashMap<>();
    configFileRootNodeMap.forEach(
        (configFile, rootNode) -> {
          Map<String, Node> uriServiceConfigNodeMap = new HashMap<>();
          for (int i = 0; i < rootNode.getChildNodes().getLength(); i++) {
            Node node = rootNode.getChildNodes().item(i);
            if (PLUGINS_NODE_NAME.equals(node.getLocalName())) {
              NodeList nodeList = node.getChildNodes();
              for (int j = 0; j < nodeList.getLength(); j++) {
                Node configNode = nodeList.item(j);
                if (configNode.getChildNodes().getLength() > 0) {
                  Node configServiceNode = configNode.getChildNodes().item(0);
                  String uri = configServiceNode.getNamespaceURI();
                  if (uriServiceConfigNodeMap.containsKey(uri)) {
                    throw new InvalidInputException(
                        ErrorCode.SAME_SERVICE_DEFINED_MULTIPLE_TIMES,
                        "URI" + uri + " has multiple service configuration",
                        tuple2(ErrorParamKey.URI.name(), uri)
                    );
                  }
                  uriServiceConfigNodeMap.put(configServiceNode.getNamespaceURI(), configServiceNode);
                }
              }
            }
          }
          configAndServiceNodesPerConfigFile.put(configFile, uriServiceConfigNodeMap);
        }
    );
    return configAndServiceNodesPerConfigFile;
  }

  protected Supplier<ValidationWrapper> getValidatorSupplier(String namespace) {
    return () -> new ValidationWrapper(TCConfigurationParser.getValidator(URI.create(namespace)));
  }

  protected void createServerConfigMapFunction(Map<Tuple2<Integer, String>, Node> stripeServerConfigMapNode, int stripeId, Path configFilePath) {
    try {
      if (regularFile(configFilePath)) {
        Node element = getRootNode(configFilePath);
        configFileRootNodeMap.put(configFilePath, element);
        List<String> serverNames = extractServerNames(element);
        checkUniqueServerNamesInStripe(serverNames, stripeId, configFilePath);
        allServers.addAll(serverNames);
        serverNames.forEach(s -> stripeServerConfigMapNode.put(tuple2(stripeId, s), getClonedParentDocNode(element)));
      } else {
        throw new InvalidInputException(
            ErrorCode.INVALID_FILE_TYPE,
            "Provided file " + configFilePath.toString() + " is not a regular file",
            tuple2(ErrorParamKey.INVALID_FILE_TYPE.name(), configFilePath.toString())
        );
      }
    } catch (ConfigConversionException e) {
      throw e;
    } catch (Exception e) {
      String errorMessage = "Unexpected error while migrating the configuration files: " + e.getMessage();
      LOGGER.debug(errorMessage, e);
      throw new ConfigConversionException(ErrorCode.UNKNOWN_ERROR, errorMessage);
    }
  }

  protected void handlePlatformPersistence(Map<Path, Node> configurationFileFileRootNodeMap) {
    configurationFileFileRootNodeMap.forEach(handlePlatformPersistencePerConfigurationFile());
  }

  protected BiConsumer<Path, Node> handlePlatformPersistencePerConfigurationFile() {
    return (Path configFilePath, Node rootNode) -> {
      Node dataRootNode = null;
      Node platformPersistenceNode = null;
      for (int i = 0; i < rootNode.getChildNodes().getLength(); i++) {
        Node node = rootNode.getChildNodes().item(i);
        if (PLUGINS_NODE_NAME.equals(node.getLocalName())) {
          NodeList nodeList = node.getChildNodes();
          for (int j = 0; j < nodeList.getLength(); j++) {
            Node configNode = nodeList.item(j);
            if (configNode.getChildNodes().getLength() > 0) {
              Node configServiceNode = configNode.getChildNodes().item(0);
              String uri = configServiceNode.getNamespaceURI();
              if (NS_DATA_ROOT.equals(uri)) {
                dataRootNode = configServiceNode;
              } else if (NS_PLATFORM_PERSISTENCE.equals(uri)) {
                platformPersistenceNode = configServiceNode;
              }
            }
          }
        }
      }
      remapPlatformPersistence(dataRootNode, platformPersistenceNode, configFilePath);
    };
  }

  protected void remapPlatformPersistence(Node dataRootNode, Node platformPersistenceNode, Path configFilePath) {
    if (platformPersistenceNode != null) {
      if (dataRootNode == null || dataRootNode.getChildNodes().getLength() == 0) {
        throw new InvalidInputConfigurationContentException(
            ErrorCode.NO_DATA_DIR_WITH_PLATFORM_PERSISTENCE,
            "Platform persistence points to a data-directory which is not present in the configuration file",
            tuple2(ErrorParamKey.CONFIG_FILE.name(), configFilePath.toString()));
      }
      String dataDirId = getAttributeValue(platformPersistenceNode, PLATFORM_PERSISTENCE_DATA_DIR_ID_ATTR_NAME);
      NodeList dataDirectories = dataRootNode.getChildNodes();
      boolean dataDirMatched = false;
      for (int i = 0; i < dataDirectories.getLength(); i++) {
        Node currentDataRootNode = dataDirectories.item(i);
        String dataRootName = getAttributeValue(currentDataRootNode, NAME_ATTR_NAME);
        if (dataDirId.equals(dataRootName)) {
          String useForPlatformString = getAttributeValue(currentDataRootNode, DATA_DIR_USER_FOR_PLATFORM_ATTR_NAME, false);
          boolean useForPlatform = Boolean.parseBoolean(useForPlatformString);
          if (!useForPlatform) {
            setAttributeValue(currentDataRootNode, DATA_DIR_USER_FOR_PLATFORM_ATTR_NAME, Boolean.TRUE.toString());
          }
          dataDirMatched = true;
          break;
        }
      }
      if (!dataDirMatched) {
        throw new InvalidInputConfigurationContentException(
            ErrorCode.INVALID_DATA_DIR_FOR_PLATFORM_PERSISTENCE,
            "Name for the data-directory missing",
            tuple2(ErrorParamKey.CONFIG_FILE.name(), configFilePath.toString()));
      }
      removeNode(platformPersistenceNode, true);
    }
  }

  protected void checkUniqueServerNamesInStripe(List<String> serverNames, int stripeId, Path configFilePath) {
    Collection<String> duplicates = serverNames.stream()
        .collect(groupingBy(identity(), counting()))
        .entrySet().stream()
        .filter(e -> e.getValue() > 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(TreeSet::new));

    if (!duplicates.isEmpty()) {
      throw new InvalidInputConfigurationContentException(
          ErrorCode.DUPLICATE_SERVER_NAME_IN_STRIPE,
          format(
              "Duplicate server names %s in configuration file %s for stripe %s",
              join(",", duplicates),
              configFilePath,
              stripeId
          )
      );
    }
  }

  /*
   * Validates if the configuration files provided are indeed part of existing valid cluster
   */
  protected void validateProvidedConfiguration(Map<Tuple2<Integer, String>, Node> hostConfigMapNode, List<String> allServers) {
    List<String> serversInAllStripesAsInput = hostConfigMapNode.keySet()
        .stream()
        .map(Tuple2::getT2)
        .collect(toList());

    if (serversInAllStripesAsInput.size() != allServers.size()) {
      @SuppressWarnings("unchecked")
      Tuple2<String, String>[] servers = (Tuple2<String, String>[]) Array.newInstance(Tuple2.class
          , serversInAllStripesAsInput.size() + allServers.size());

      AtomicInteger counter = new AtomicInteger(0);
      serversInAllStripesAsInput.forEach(serverInInput -> servers[counter.getAndIncrement()] = tuple2(ErrorParamKey.SERVERS_IN_COMMAND.name(), serverInInput));

      allServers.forEach(server -> servers[counter.getAndIncrement()] = tuple2(ErrorParamKey.SERVERS_IN_CONFIG_FILES.name(), server));

      throw new InvalidInputException(
          ErrorCode.MISSING_SERVERS,
          format(
              "Not all servers are provided. Servers provided as input are: %s, whereas servers present in configuration files are: %s ",
              join(",", serversInAllStripesAsInput),
              join(",", allServers)
          ),
          servers);
    }

    Collection<String> mismatched = mismatchedServers(hostConfigMapNode, allServers);
    if (mismatched.size() > 0) {
      String mismatchedString = join(",", mismatched);
      throw new InvalidInputException(
          ErrorCode.MISMATCHED_SERVERS,
          "Mismatched servers are : " + mismatchedString,
          tuple2(ErrorParamKey.MISMATCHED_SERVERS.name(), mismatchedString)
      );
    }
  }

  protected Collection<String> mismatchedServers(Map<Tuple2<Integer, String>, Node> hostConfigMapNode, List<String> allServers) {
    return hostConfigMapNode.keySet().stream()
        .map(pair -> {
          String serverName = pair.getT2();
          return !allServers.contains(serverName) ? serverName : null;
        })
        .filter(Objects::nonNull)
        .collect(toList());
  }

  protected boolean regularFile(Path path) {
    return Files.isRegularFile(path);
  }

  protected Cluster getCluster(String clusterName, Map<Tuple2<Integer, String>, Node> nodeNameNodeConfigMap, List<String> stripeNames) {
    validateProvidedConfiguration(nodeNameNodeConfigMap, allServers);
    /* We want to eventually create cluster object so we can remove repeated parsing since all servers
     in same stripe will have same stripe level configuration. */

    Map<Integer, Node> oneConfigPerStripe = new HashMap<>();
    for (Map.Entry<Tuple2<Integer, String>, Node> entry : nodeNameNodeConfigMap.entrySet()) {
      if (!oneConfigPerStripe.containsKey(entry.getKey().t1)) {
        oneConfigPerStripe.put(entry.getKey().t1, entry.getValue());
      }
    }

    List<Cluster> stripes = new ArrayList<>();
    for (Map.Entry<Integer, Node> entry : oneConfigPerStripe.entrySet()) {
      Node doc = entry.getValue();
      try {
        String xml = XmlUtility.getPrettyPrintableXmlString(doc);
        Cluster stripe = getStripe(xml);
        stripes.add(stripe);
      } catch (TransformerException e) {
        throw new RuntimeException(e);
      }
    }
    final Cluster cluster = stripes.stream().reduce((result, stripe) -> result
        .addStripe(stripe.getSingleStripe().get().clone())) // getSingleStripe() because conversion of xml -> model is for 1 stripe only
        .orElseThrow(() -> new RuntimeException("No server specified."))
        .setName(clusterName);

    // add UIDs
    cluster.setUID(cluster.newUID());
    cluster.getStripes().forEach(stripe -> {
      stripe.setUID(cluster.newUID());
      stripe.getNodes().forEach(node -> node.setUID(cluster.newUID()));
    });

    // assign stripe names based on user input
    for (int i = 0, max = Math.min(cluster.getStripeCount(), stripeNames.size()); i < max; i++) {
      cluster.getStripes().get(i).setName(stripeNames.get(i));
    }

    // for remaining names not assigned, generate them
    NameGenerator.assignFriendlyNames(cluster);

    return cluster;
  }
}
