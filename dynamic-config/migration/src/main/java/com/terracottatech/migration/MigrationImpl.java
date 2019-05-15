/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.terracottatech.migration.exception.ErrorCode;
import com.terracottatech.migration.exception.ErrorParamKey;
import com.terracottatech.migration.exception.InvalidInputConfigurationContentException;
import com.terracottatech.migration.exception.InvalidInputException;
import com.terracottatech.migration.exception.MigrationException;
import com.terracottatech.migration.util.Pair;
import com.terracottatech.migration.util.XmlUtility;
import com.terracottatech.migration.validators.ValidationWrapper;
import com.terracottatech.migration.validators.ValidatorFactory;
import com.terracottatech.topology.config.ClusteredConfigBuilder;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.terracottatech.migration.exception.ErrorCode.UNKNOWN_ERROR;
import static java.util.stream.Collectors.toList;

/*
```
cluster-tool.sh convert-config -tt <topology-type> -tn <topology-name> -c <update-string> -c <update-string> -d <dir>

```
.. Input
... <topology-name> name of the cluster
.....<update-string> <stripe-name, path to tc-config.xml>
 */

public class MigrationImpl implements Migration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationImpl.class);

  static final String TERRACOTTA_CONFIG_NAMESPACE = "http://www.terracotta.org/config";
  static final String NAME_NODE_NAME = "name";
  static final String SERVERS_NODE_NAME = "servers";
  static final String SERVER_NODE_NAME = "server";

  static final String PLUGINS_NODE_NAME = "plugins";

  private final Map<Path, Node> configFileRootNodeMap = new HashMap<>();
  private final List<String[]> inputParamList = new ArrayList<>();
  private final List<String> allServers = new ArrayList<>();
  private final Map<String, List<String>> stripeServerNameMap = new HashMap<>();
  private final NodeConfigurationHandler repositoryBuilder;

  public MigrationImpl(NodeConfigurationHandler nodeConfigurationHandler) {
    this.repositoryBuilder = nodeConfigurationHandler;
  }

  public MigrationImpl() {
    this(null);
  }

  public void processInput(String topologyName, List<String> migrationStrings) {
    //Basic validation for inputs
    LOGGER.info("Starting to validate input command parameters");
    validateAndProcessInput(migrationStrings);
    LOGGER.info("Validated command parameters");
    Map<String, Path> configFilePerStripeMap = new HashMap<>();
    Map<Pair<String, String>, Node> stripeServerConfigNodeMap = new HashMap<>();
    for (String[] inputStringArray : inputParamList) {
      String stripeName = inputStringArray[0];
      configFilePerStripeMap.put(stripeName, Paths.get(inputStringArray[1]));
    }

    for (Map.Entry<String, Path> stringPathEntry : configFilePerStripeMap.entrySet()) {
      createServerConfigMapFunction(stripeServerConfigNodeMap, stringPathEntry.getKey(), stringPathEntry.getValue());
    }
    LOGGER.info("Validating contents of the configuration files");
    valueValidators();
    LOGGER.info("Building Cluster");
    buildCluster(topologyName, stripeServerConfigNodeMap);
  }

  protected void validateAndProcessInput(List<String> migrationStrings) {
    boolean generateStripeName = validateInputAndDecideSyntheticStripeNameGeneration(migrationStrings);
    Set<String> discoveredStripeNames = new HashSet<>();
    int stripeIndex = 0;
    for (String input : migrationStrings) {
      String[] inputStringArray = input.split(",");
      String stripeName;
      if (generateStripeName) {
        String configFileName = inputStringArray[0];
        stripeName = "stripe-" + (++stripeIndex);
        String[] stripeConfigFile = new String[2];
        stripeConfigFile[0] = stripeName;
        stripeConfigFile[1] = configFileName;
        inputParamList.add(stripeConfigFile);
      } else {
        stripeName = inputStringArray[0];
        /*Same stripe name should not be present in comma separated commands
        For example we cannot have command strings like stripe1,tc-config-1.xml stripe1,tc-config-2.xml present.
        */
        if (discoveredStripeNames.contains(stripeName)) {
          throw new InvalidInputException(ErrorCode.DUPLICATE_STRIPE_NAME,
              "Duplicate stripe name " + stripeName + " in input command",
              new Pair<>(ErrorParamKey.STRIPE_NAME.name(), stripeName));
        }
        discoveredStripeNames.add(stripeName);
        inputParamList.add(inputStringArray);
      }
    }
  }

  protected boolean validateInputAndDecideSyntheticStripeNameGeneration(List<String> migrationStrings) {
    Objects.requireNonNull(migrationStrings, "Null Input String");
    boolean doNotGenerateStripeName = false;
    boolean generateStripeName = false;
    for (String input : migrationStrings) {
      String[] inputStringArray = input.split(",");
      if (inputStringArray.length != 1 && inputStringArray.length != 2) {
        throw new InvalidInputException(ErrorCode.INVALID_INPUT_PATTERN, "Invalid Input " + input);
      }
      if (inputStringArray.length == 1) {
        if (doNotGenerateStripeName) {
          String errorMessage = migrationStrings.stream().collect(Collectors.joining(" "));
          throw new InvalidInputException(ErrorCode.INVALID_MIXED_INPUT_PATTERN, "Invalid Input " + errorMessage);
        }
        generateStripeName = true;
      }
      if (inputStringArray.length == 2) {
        if (generateStripeName) {
          String errorMessage = migrationStrings.stream().collect(Collectors.joining(" "));
          throw new InvalidInputException(ErrorCode.INVALID_MIXED_INPUT_PATTERN, "Invalid Input " + errorMessage);
        }
        doNotGenerateStripeName = true;
      }
    }
    return generateStripeName;
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
    List<Pair<Map<Path, Node>, ValidationWrapper>> validatorsWithParams =
        prepareValidatorsForPluginConfigurations(namespaces, configAndServiceNodesPerConfigFile);

    validatePluginConfigurations(validatorsWithParams);

  }

  protected Set<String> validateAllConfigurationFilesHaveSamePluginTypes(Map<Path
      , Map<String, Node>> configAndServiceNodesPerConfigFile) {
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
        throw new InvalidInputConfigurationContentException(ErrorCode.MISMATCHED_SERVICE_CONFIGURATION
            , "Mismatched Service Configuration"
            , new Pair<>(ErrorParamKey.CONFIG_FILE.name(), path.toString())
            , new Pair<>(ErrorParamKey.CONFIG_FILE.name(), previousPath.get()
            .toString()));
      }
      previousSetReference.set(nodeMap.keySet());
    });
    Set<String> namespaces = previousSetReference.get();
    return namespaces;
  }

  protected List<Pair<Map<Path, Node>, ValidationWrapper>> prepareValidatorsForPluginConfigurations(Set<String> namespaces
      , Map<Path, Map<String, Node>> configAndServiceNodesPerConfigFile) {
    List<Pair<Map<Path, Node>, ValidationWrapper>> validatorsWithParams = new ArrayList<>();
    namespaces.forEach(namespace -> {
      Map<Path, Node> configFilesAndNodesMap = new HashMap<>();
      configAndServiceNodesPerConfigFile.forEach((path, nodeMap) -> {

        /*
        For a give namespace, create a map of config-file -> plugin-configuration node present in that config-file
        */

        Node node = nodeMap.get(namespace);
        configFilesAndNodesMap.put(path, node);
      });
      Supplier<ValidationWrapper> validatorSupplier = getValidatorSuppliers(namespace);
      if (validatorSupplier != null) {
        validatorsWithParams.add(new Pair<>(configFilesAndNodesMap, validatorSupplier.get()));
      }
    });
    return validatorsWithParams;
  }

  protected void validatePluginConfigurations(List<Pair<Map<Path, Node>, ValidationWrapper>> validatorsWithParams) {
    /*
    Do the validation
     */
    validatorsWithParams.forEach(pair -> {
      pair.getAnother().check(pair.getOne());
    });
  }

  protected Node getRootNode(Path configFilePath) throws Exception {
    File configFile = configFilePath.toFile();
    Node element = ConfigurationParser.getRoot(configFile
        , Thread.currentThread().getContextClassLoader());
    return element;
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

  protected String getAttributeValue(Node node, String attributeName) {
    Optional<String> attributeValue = getOptionalAttributeValue(node, attributeName);
    if (attributeValue.isPresent()) {
      return attributeValue.get();
    }
    throw new MigrationException(ErrorCode.INVALID_ATTRIBUTE_NAME, "Attribute " + attributeName + " is missing in Node " + node
        .getLocalName()
        , new Pair<>("AttributeName", attributeName)
        , new Pair<>("nodeName", node.getLocalName()));
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
                    throw new InvalidInputException(ErrorCode.SAME_SERVICE_DEFINED_MULTIPLE_TIMES
                        , "URI" + uri + " has multiple service configuration", new Pair<>(ErrorParamKey.URI
                        .name(), uri));
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

  protected Supplier<ValidationWrapper> getValidatorSuppliers(String namespace) {
    return ValidatorFactory.getParserValidatorValidator(URI.create(namespace));
  }

  protected void createServerConfigMapFunction(Map<Pair<String, String>
      , Node> stripeServerConfigMapNode, String stripeName, Path configFilePath) {
    try {
      if (regularFile(configFilePath)) {
        Node element = getRootNode(configFilePath);
        configFileRootNodeMap.put(configFilePath, element);
        List<String> serverNames = extractServerNames(element);
        checkUniqueServerNamesInStripe(serverNames, stripeName, configFilePath);
        allServers.addAll(serverNames);
        serverNames.forEach(s -> stripeServerConfigMapNode.put(new Pair<>(stripeName, s)
            , getClonedParentDocNode(element)));
        stripeServerNameMap.put(stripeName, serverNames);
      } else {
        throw new InvalidInputException(ErrorCode.INVALID_FILE_TYPE, "Invalid file. Provided file is not a regular file"
            , new Pair<>(ErrorParamKey.INVALID_FILE_TYPE.name(), configFilePath.toString()));
      }
    } catch (MigrationException e) {
      throw  e;
    } catch (Exception e) {
      String errorMessage = "Unexpected error while migrating the configuration files";
      LOGGER.error(errorMessage, e);
      throw new MigrationException(UNKNOWN_ERROR, errorMessage);
    }
  }

  protected void checkUniqueServerNamesInStripe(List<String> serverNames, String stripeNme, Path configFilePath) {
    List<String> serverNamesLocal = new ArrayList<>(serverNames);
    Set<String> distinctServerNames = serverNames.stream().distinct().collect(Collectors.toSet());
    if (distinctServerNames.size() != serverNames.size()) {
      for (String uniqueServerName : serverNames) {
        serverNamesLocal.remove(uniqueServerName);
      }
      String duplicateServerNames = String.join(",", serverNamesLocal);
      throw new InvalidInputConfigurationContentException(ErrorCode.DUPLICATE_SERVER_NAME_IN_STRIPE,
          "Duplicate server names " + duplicateServerNames + "in configuration file " + configFilePath + "for stripe "
          + stripeNme);
    }
  }


  public void buildCluster(String clusterName, Map<Pair<String, String>, Node> hostConfigMapNode) {
    /*
    Validates if the configuration files provided are indeed part of existing valid cluster
     */
    validateProvidedConfiguration(hostConfigMapNode, allServers);
    ClusteredConfigBuilder clusteredConfigBuilder = new ClusteredConfigBuilder(hostConfigMapNode, stripeServerNameMap);
    clusteredConfigBuilder.createEntireCluster(clusterName);

    if (repositoryBuilder != null) {
      repositoryBuilder.process(hostConfigMapNode);
    }

  }

  protected void validateProvidedConfiguration(Map<Pair<String, String>, Node> hostConfigMapNode
      , List<String> allServers) {
    List<String> serversInAllStripesAsInput = hostConfigMapNode.entrySet()
        .stream()
        .map(entry -> entry.getKey().getAnother())
        .collect(toList());
    if (serversInAllStripesAsInput.size() != allServers.size()) {
      String serversInConfigurationFiles = String.join(",", allServers);
      @SuppressWarnings("unchecked")
      Pair<String, String>[] servers = (Pair<String, String>[])Array.newInstance(Pair.class
          , serversInAllStripesAsInput.size() + allServers.size());

      AtomicInteger counter = new AtomicInteger(0);
      serversInAllStripesAsInput.forEach(serverInInput -> {
        servers[counter.getAndIncrement()] = new Pair<>(ErrorParamKey.SERVERS_IN_COMMAND.name(), serverInInput);
      });

      allServers.forEach(server -> {
        servers[counter.getAndIncrement()] = new Pair<>(ErrorParamKey.SERVERS_IN_CONFIG_FILES.name(), server);
      });

      String serversAsInput = String.join(",", serversInAllStripesAsInput);
      throw new InvalidInputException(ErrorCode.MISSING_SERVERS,
          "Not all servers are provided. " +
          "Servers provides as input are " + serversAsInput + ". Servers " +
          "present in configuration files are " + serversInConfigurationFiles
          , servers);
    }
    Collection<String> mismatched = mismatchedServers(hostConfigMapNode, allServers);
    if (mismatched.size() > 0) {
      String mismatchedString = String.join(",", mismatched);
      throw new InvalidInputException(ErrorCode.MISMATCHED_SERVERS,
          "Mismatched servers are : " + mismatchedString
          , new Pair<>(ErrorParamKey.MISMATCHED_SERVERS
          .name(), mismatchedString));
    }
  }

  protected Collection<String> mismatchedServers(Map<Pair<String, String>, Node> hostConfigMapNode
      , List<String> allServers) {
    return hostConfigMapNode.keySet().stream()
        .map(pair -> {
          String serverName = pair.getAnother();
          return !allServers.contains(serverName) ? serverName : null;
        })
        .filter(Objects::nonNull)
        .collect(toList());
  }

  protected boolean regularFile(Path path) {
    return Files.isRegularFile(path);
  }
}