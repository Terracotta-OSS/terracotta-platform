/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration;

import com.terracottatech.migration.exception.ErrorCode;
import com.terracottatech.migration.exception.ErrorParamKey;
import com.terracottatech.migration.exception.InvalidInputConfigurationContentException;
import com.terracottatech.migration.exception.InvalidInputException;
import com.terracottatech.migration.exception.MigrationException;
import com.terracottatech.migration.validators.ValidationWrapper;
import com.terracottatech.migration.xml.ClusteredConfigBuilder;
import com.terracottatech.migration.xml.ConfigurationParser;
import com.terracottatech.migration.xml.XmlUtility;
import com.terracottatech.struct.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.TCConfigurationParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.terracottatech.migration.exception.ErrorCode.UNKNOWN_ERROR;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class MigrationImpl implements Migration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationImpl.class);

  static final String TERRACOTTA_CONFIG_NAMESPACE = "http://www.terracotta.org/config";
  static final String NAME_NODE_NAME = "name";
  static final String SERVERS_NODE_NAME = "servers";
  static final String SERVER_NODE_NAME = "server";

  static final String PLUGINS_NODE_NAME = "plugins";
  static final String PLATFORM_PERSISTENCE_DATA_DIR_ID_ATTR_NAME = "data-directory-id";
  static final String DATA_DIR_USER_FOR_PLATFORM_ATTR_NAME = "use-for-platform";
  static final String NAME_ATTR_NAME = "name";

  private final Map<Path, Node> configFileRootNodeMap = new HashMap<>();
  private final List<String[]> inputParamList = new ArrayList<>();
  private final List<String> allServers = new ArrayList<>();
  private final Map<Integer, List<String>> stripeServerNameMap = new HashMap<>();
  private final Consumer<Map<Tuple2<Integer, String>, Node>> repositoryBuilder;

  public MigrationImpl(Consumer<Map<Tuple2<Integer, String>, Node>> nodeConfigurationHandler) {
    this.repositoryBuilder = nodeConfigurationHandler;
  }

  public MigrationImpl() {
    this(o -> {
    });
  }

  public void processInput(String clusterName, List<String> migrationStrings) {
    //Basic validation for inputs
    LOGGER.debug("Starting to validate input command parameters");
    validateAndProcessInput(migrationStrings);
    LOGGER.debug("Validated command parameters");
    Map<Integer, Path> configFilePerStripeMap = new HashMap<>();
    Map<Tuple2<Integer, String>, Node> stripeServerConfigNodeMap = new HashMap<>();
    for (String[] inputStringArray : inputParamList) {
      int stripeId = Integer.parseInt(inputStringArray[0]);
      configFilePerStripeMap.put(stripeId, Paths.get(inputStringArray[1]));
    }

    for (Map.Entry<Integer, Path> stringPathEntry : configFilePerStripeMap.entrySet()) {
      createServerConfigMapFunction(stripeServerConfigNodeMap, stringPathEntry.getKey(), stringPathEntry.getValue());
    }
    LOGGER.debug("Checking if deprecated platform-persistence needs to be converted to data-directory. Will convert if found.");
    handlePlatformPersistence(configFileRootNodeMap);
    LOGGER.debug("Validating contents of the configuration files");
    valueValidators();
    LOGGER.debug("Building Cluster");
    buildCluster(clusterName, stripeServerConfigNodeMap);
  }

  protected void validateAndProcessInput(List<String> migrationStrings) {
    boolean generateStripeId = validateInputAndDecideSyntheticStripeIdGeneration(migrationStrings);
    Set<Integer> discoveredStripeIds = new TreeSet<>();
    int stripeIndex = 0;
    for (String input : migrationStrings) {
      String[] inputStringArray = input.split(",");
      int stripeId;
      if (generateStripeId) {
        String configFileName = inputStringArray[0];
        stripeId = ++stripeIndex;
        String[] stripeConfigFile = new String[2];
        stripeConfigFile[0] = String.valueOf(stripeId);
        stripeConfigFile[1] = configFileName;
        inputParamList.add(stripeConfigFile);
      } else {
        stripeId = Integer.parseInt(inputStringArray[0]);
        /*Same stripe name should not be present in comma separated commands
        For example we cannot have command strings like stripe1,tc-config-1.xml stripe1,tc-config-2.xml present.
        */
        if (discoveredStripeIds.contains(stripeId)) {
          throw new InvalidInputException(ErrorCode.DUPLICATE_STRIPE_NAME,
              "Duplicate stripe ID " + stripeId + " in input command",
              Tuple2.tuple2(ErrorParamKey.STRIPE_ID.name(), String.valueOf(stripeId)));
        }
        discoveredStripeIds.add(stripeId);
        inputParamList.add(inputStringArray);
      }
    }
  }

  private boolean validateInputAndDecideSyntheticStripeIdGeneration(List<String> migrationStrings) {
    Objects.requireNonNull(migrationStrings, "Null Input String");
    boolean doNotGenerateStripeId = false;
    boolean generateStripeId = false;
    for (String input : migrationStrings) {
      String[] inputStringArray = input.split(",");
      if (inputStringArray.length != 1 && inputStringArray.length != 2) {
        throw new InvalidInputException(ErrorCode.INVALID_INPUT_PATTERN, "Invalid Input " + input);
      }
      if (inputStringArray.length == 1) {
        if (doNotGenerateStripeId) {
          throw new InvalidInputException(
              ErrorCode.INVALID_MIXED_INPUT_PATTERN,
              "Invalid Input " + join(" ", migrationStrings)
          );
        }
        generateStripeId = true;
      }
      if (inputStringArray.length == 2) {
        if (generateStripeId) {
          throw new InvalidInputException(
              ErrorCode.INVALID_MIXED_INPUT_PATTERN,
              "Invalid Input " + join(" ", migrationStrings)
          );
        }
        doNotGenerateStripeId = true;
      }
    }
    return generateStripeId;
  }

  private void valueValidators() {
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
            Tuple2.tuple2(ErrorParamKey.CONFIG_FILE.name(), path.toString()),
            Tuple2.tuple2(ErrorParamKey.CONFIG_FILE.name(), previousPath.get().toString())
        );
      }
      previousSetReference.set(nodeMap.keySet());
    });
    return previousSetReference.get();
  }

  private List<Tuple2<Map<Path, Node>, ValidationWrapper>> prepareValidatorsForPluginConfigurations(Set<String> namespaces
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
      validatorsWithParams.add(Tuple2.tuple2(configFilesAndNodesMap, validatorSupplier.get()));
    });
    return validatorsWithParams;
  }

  protected void validatePluginConfigurations(List<Tuple2<Map<Path, Node>, ValidationWrapper>> validatorsWithParams) {
    validatorsWithParams.forEach(pair -> pair.getT2().check(pair.getT1()));
  }

  protected Node getRootNode(Path configFilePath) throws Exception {
    File configFile = configFilePath.toFile();
    return ConfigurationParser.getRoot(configFile, Thread.currentThread().getContextClassLoader());
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
      throw new MigrationException(
          ErrorCode.INVALID_ATTRIBUTE_NAME,
          "Attribute " + attributeName + " is missing in Node " + node.getLocalName(),
          Tuple2.tuple2("AttributeName", attributeName),
          Tuple2.tuple2("nodeName", node.getLocalName())
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

  private Optional<String> getOptionalAttributeValue(Node node, String attributeName) {
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
                        Tuple2.tuple2(ErrorParamKey.URI.name(), uri)
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

  private Supplier<ValidationWrapper> getValidatorSupplier(String namespace) {
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
        serverNames.forEach(s -> stripeServerConfigMapNode.put(Tuple2.tuple2(stripeId, s), getClonedParentDocNode(element)));
        stripeServerNameMap.put(stripeId, serverNames);
      } else {
        throw new InvalidInputException(
            ErrorCode.INVALID_FILE_TYPE,
            "Provided file " + configFilePath.toString() + " is not a regular file",
            Tuple2.tuple2(ErrorParamKey.INVALID_FILE_TYPE.name(), configFilePath.toString())
        );
      }
    } catch (MigrationException e) {
      throw e;
    } catch (Exception e) {
      String errorMessage = "Unexpected error while migrating the configuration files: " + e.getMessage();
      LOGGER.debug(errorMessage, e);
      throw new MigrationException(UNKNOWN_ERROR, errorMessage);
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
              if (XmlUtility.getDataRootsNamespace().equals(uri)) {
                dataRootNode = configServiceNode;
              } else if (XmlUtility.getPlatformPersistenceNamespace().equals(uri)) {
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
            Tuple2.tuple2(ErrorParamKey.CONFIG_FILE.name(), configFilePath.toString()));
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
            Tuple2.tuple2(ErrorParamKey.CONFIG_FILE.name(), configFilePath.toString()));
      }
      removeNode(platformPersistenceNode, true);
    }
  }

  private void checkUniqueServerNamesInStripe(List<String> serverNames, int stripeId, Path configFilePath) {
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


  private void buildCluster(String clusterName, Map<Tuple2<Integer, String>, Node> hostConfigMapNode) {
    validateProvidedConfiguration(hostConfigMapNode, allServers);
    ClusteredConfigBuilder clusteredConfigBuilder = new ClusteredConfigBuilder(hostConfigMapNode, stripeServerNameMap);
    clusteredConfigBuilder.createEntireCluster(clusterName);

    if (repositoryBuilder != null) {
      repositoryBuilder.accept(hostConfigMapNode);
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
      serversInAllStripesAsInput.forEach(serverInInput -> {
        servers[counter.getAndIncrement()] = Tuple2.tuple2(ErrorParamKey.SERVERS_IN_COMMAND.name(), serverInInput);
      });

      allServers.forEach(server -> {
        servers[counter.getAndIncrement()] = Tuple2.tuple2(ErrorParamKey.SERVERS_IN_CONFIG_FILES.name(), server);
      });

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
          Tuple2.tuple2(ErrorParamKey.MISMATCHED_SERVERS.name(), mismatchedString)
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
}