/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.terracotta.config.BindPort;
import org.terracotta.config.Config;
import org.terracotta.config.Server;
import org.terracotta.config.Service;
import org.terracotta.config.Services;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.w3c.dom.Element;

import com.terracottatech.migration.util.Pair;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.topology.config.xmlobjects.Cluster;
import com.terracottatech.topology.config.xmlobjects.Node;
import com.terracottatech.topology.config.xmlobjects.ServerConfig;
import com.terracottatech.topology.config.xmlobjects.Stripe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;

public class MigrationIT {

  private static Path dataDirectoryRoot;
  private static boolean deleteAtExit = false;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @BeforeClass
  public static void createDir() {
    Path rootPath = Paths.get(".").toAbsolutePath();
    dataDirectoryRoot = rootPath.resolve("test").normalize();
    try {
      deleteAtExit = !ensureDirectory(dataDirectoryRoot);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create data directory: " + dataDirectoryRoot, e);
    }
  }

  @AfterClass
  public static void deleteDir() throws IOException {
    if (deleteAtExit) {
      Files.walk(dataDirectoryRoot)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  static boolean ensureDirectory(Path directory) throws IOException {
    boolean directoryAlreadyExists = true;
    if (!Files.exists(directory)) {
      Files.createDirectories(directory);
      directoryAlreadyExists = false;
    } else {
      if (!Files.isDirectory(directory)) {
        throw new RuntimeException("A file with configured data directory (" + directory + ") already exists!");
      }
    }
    return directoryAlreadyExists;
  }

  @Test
  public void testSingleStripeSingleFile() throws Exception {
    Map<String, NomadServer> serverMap = new HashMap<>();

    Path outputFolderPath = folder.newFolder().toPath();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor);

    Path inputFilePath = Paths.get(MigrationIT.class.getResource("/tc-config-single-server.xml").toURI());
    String inputFileLocation = inputFilePath.toString();
    migration.processInput("testCluster", Arrays.asList("stripe1" + "," + inputFileLocation));
    List<Path> subdirectory;
    try (Stream<Path> filePathStream = Files.walk(outputFolderPath)) {
      subdirectory = filePathStream.filter(Files::isDirectory)
          .filter(filePath -> !outputFolderPath.equals(filePath))
          .collect(Collectors.toList());
    }
    subdirectory.forEach(filePath -> {
      if (!outputFolderPath.equals(filePath)) {
        File file = filePath.toFile();
        if (file.isDirectory() && file.getParentFile().toPath().equals(outputFolderPath)) {
          String directoryName = file.getName();
          assertThat(directoryName, is("stripe1_server-1"));
        }
      }
    });

    NomadServer nomadServer = serverMap.get("stripe1_server-1");
    DiscoverResponse discoverResponse = nomadServer.discover();

    String convertedConfigContent = discoverResponse.getLatestChange().getResult();
    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent);
    assertThat(configuration, notNullValue());

    List<Cluster> clusterList = configuration.getExtendedConfiguration(Cluster.class);
    assertThat(clusterList, notNullValue());
    assertThat(clusterList.size(), is(1));

    Cluster cluster = clusterList.get(0);
    assertThat(cluster, notNullValue());

    assertThat(cluster.getName(), is("testCluster"));
    List<Stripe> stripeList = cluster.getStripes();
    assertThat(stripeList.size(), is(1));

    Stripe stripe = stripeList.get(0);
    assertThat(stripe, notNullValue());

    assertThat(stripe.getName(), is("stripe1"));
    List<Node> nodes = stripe.getNodes();
    assertThat(nodes.size(), is(1));
    assertThat(nodes.get(0).getName(), is("server-1"));

    ServerConfig serverConfig = nodes.get(0).getServerConfig();
    assertThat(serverConfig, notNullValue());

    TcConfig clusterTcConfig = serverConfig.getTcConfig();
    assertThat(clusterTcConfig, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();
    assertThat(tcConfig.getFailoverPriority(), notNullValue());
    assertThat(clusterTcConfig.getFailoverPriority(), notNullValue());
    assertThat(tcConfig.getTcProperties(), nullValue());
    assertThat(clusterTcConfig.getTcProperties(), nullValue());
    assertThat(tcConfig.getServers(), notNullValue());
    assertThat(clusterTcConfig.getServers(), notNullValue());
    assertThat(tcConfig.getServers().getClientReconnectWindow(), notNullValue());
    assertThat(clusterTcConfig.getServers().getClientReconnectWindow(), notNullValue());
    assertThat(clusterTcConfig.getServers()
        .getClientReconnectWindow(), is(tcConfig.getServers().getClientReconnectWindow()));

    List<Server> serverList = tcConfig.getServers().getServer();
    List<Server> serverListCluster = clusterTcConfig.getServers().getServer();
    assertThat(serverList, notNullValue());
    assertThat(serverListCluster, notNullValue());
    assertThat(serverListCluster.size(), is(serverList.size()));

    Server server = serverList.get(0);
    Server serverCluster = serverListCluster.get(0);
    assertThat(serverCluster.getHost(), is(server.getHost()));
    assertThat(serverCluster.getBind(), is(server.getBind()));
    Services clusterServices = clusterTcConfig.getPlugins();
    assertThat(clusterServices, notNullValue());

    List<Object> servicesOrConfigs = clusterServices.getConfigOrService();
    assertThat(servicesOrConfigs, notNullValue());
    assertThat(servicesOrConfigs.size(), is(2));

    servicesOrConfigs.forEach(object -> {
      assertThat((object instanceof Service || object instanceof Config), is(true));
      if (object instanceof Service) {
        Service service = (Service)object;
        Element serviceContent = service.getServiceContent();
        assertThat(serviceContent, notNullValue());
      } else {
        Config config = (Config)object;
        Element configContent = config.getConfigContent();
        assertThat(configContent, notNullValue());
      }
    });
  }

  @Test
  public void testSingleStripeSingleFileNoStripeName() throws Exception {
    Map<String, NomadServer> serverMap = new HashMap<>();
    Path outputFolderPath = folder.newFolder().toPath();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor);

    Path inputFilePath = Paths.get(MigrationIT.class.getResource("/tc-config-single-server.xml").toURI());
    String inputFileLocation = inputFilePath.toString();
    migration.processInput("testCluster", Arrays.asList(inputFileLocation));
    List<Path> subdirectory;
    try (Stream<Path> filePathStream = Files.walk(outputFolderPath)) {
      subdirectory = filePathStream.filter(Files::isDirectory)
          .filter(filePath -> !outputFolderPath.equals(filePath))
          .collect(Collectors.toList());
    }
    subdirectory.forEach(filePath -> {
      if (!outputFolderPath.equals(filePath)) {
        File file = filePath.toFile();
        if (file.isDirectory() && file.getParentFile().toPath().equals(outputFolderPath)) {
          String directoryName = file.getName();
          assertThat(directoryName, is("stripe-1_server-1"));
        }
      }
    });

    NomadServer nomadServer = serverMap.get("stripe-1_server-1");
    DiscoverResponse discoverResponse = nomadServer.discover();

    String convertedConfigContent = discoverResponse.getLatestChange().getResult();

    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent);
    assertThat(configuration, notNullValue());

    List<Cluster> clusterList = configuration.getExtendedConfiguration(Cluster.class);
    assertThat(clusterList, notNullValue());
    assertThat(clusterList.size(), is(1));

    Cluster cluster = clusterList.get(0);
    assertThat(cluster, notNullValue());

    assertThat(cluster.getName(), is("testCluster"));
    List<Stripe> stripeList = cluster.getStripes();
    assertThat(stripeList.size(), is(1));

    Stripe stripe = stripeList.get(0);
    assertThat(stripe, notNullValue());

    assertThat(stripe.getName(), is("stripe-1"));
    List<Node> nodes = stripe.getNodes();
    assertThat(nodes.size(), is(1));
    assertThat(nodes.get(0).getName(), is("server-1"));

    ServerConfig serverConfig = nodes.get(0).getServerConfig();
    assertThat(serverConfig, notNullValue());

    TcConfig clusterTcConfig = serverConfig.getTcConfig();
    assertThat(clusterTcConfig, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();
    assertThat(tcConfig.getFailoverPriority(), notNullValue());
    assertThat(clusterTcConfig.getFailoverPriority(), notNullValue());
    assertThat(tcConfig.getTcProperties(), nullValue());
    assertThat(clusterTcConfig.getTcProperties(), nullValue());
    assertThat(tcConfig.getServers(), notNullValue());
    assertThat(clusterTcConfig.getServers(), notNullValue());
    assertThat(tcConfig.getServers().getClientReconnectWindow(), notNullValue());
    assertThat(clusterTcConfig.getServers().getClientReconnectWindow(), notNullValue());
    assertThat(clusterTcConfig.getServers()
        .getClientReconnectWindow(), is(tcConfig.getServers().getClientReconnectWindow()));

    List<Server> serverList = tcConfig.getServers().getServer();
    List<Server> serverListCluster = clusterTcConfig.getServers().getServer();
    assertThat(serverList, notNullValue());
    assertThat(serverListCluster, notNullValue());
    assertThat(serverListCluster.size(), is(serverList.size()));

    Server server = serverList.get(0);
    Server serverCluster = serverListCluster.get(0);
    assertThat(serverCluster.getHost(), is(server.getHost()));
    assertThat(serverCluster.getBind(), is(server.getBind()));

    Services clusterServices = clusterTcConfig.getPlugins();
    assertThat(clusterServices, notNullValue());

    List<Object> servicesOrConfigs = clusterServices.getConfigOrService();
    assertThat(servicesOrConfigs, notNullValue());
    assertThat(servicesOrConfigs.size(), is(2));

    servicesOrConfigs.forEach(object -> {
      assertThat((object instanceof Service || object instanceof Config), is(true));
      if (object instanceof Service) {
        Service service = (Service)object;
        Element serviceContent = service.getServiceContent();
        assertThat(serviceContent, notNullValue());
      } else {
        Config config = (Config)object;
        Element configContent = config.getConfigContent();
        assertThat(configContent, notNullValue());
      }
    });
  }

  @Test
  public void testSingleStripeSingleFileWithSecurity() throws Exception {
    Map<String, NomadServer> serverMap = new HashMap<>();
    Path outputFolderPath = folder.newFolder().toPath();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor);

    Path inputFilePath = Paths.get(MigrationIT.class.getResource("/tc-config-single-server-with-security.xml").toURI());
    String inputFileLocation = inputFilePath.toString();
    migration.processInput("testCluster", Arrays.asList("stripe1" + "," + inputFileLocation));
    List<Path> subdirectory;
    try (Stream<Path> filePathStream = Files.walk(outputFolderPath)) {
      subdirectory = filePathStream.filter(Files::isDirectory)
          .filter(filePath -> !outputFolderPath.equals(filePath))
          .collect(Collectors.toList());
    }
    subdirectory.forEach(filePath -> {
      if (!outputFolderPath.equals(filePath)) {
        File file = filePath.toFile();
        if (file.isDirectory() && file.getParentFile().toPath().equals(outputFolderPath)) {
          String directoryName = file.getName();
          assertThat(directoryName, is("stripe1_server-1"));
        }
      }
    });


    NomadServer nomadServer = serverMap.get("stripe1_server-1");
    DiscoverResponse discoverResponse = nomadServer.discover();
    String convertedConfigContent = discoverResponse.getLatestChange().getResult();
    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent);
    assertThat(configuration, notNullValue());

    List<Cluster> clusterList = configuration.getExtendedConfiguration(Cluster.class);
    assertThat(clusterList, notNullValue());
    assertThat(clusterList.size(), is(1));

    Cluster cluster = clusterList.get(0);
    assertThat(cluster, notNullValue());

    assertThat(cluster.getName(), is("testCluster"));
    List<Stripe> stripeList = cluster.getStripes();
    assertThat(stripeList.size(), is(1));

    Stripe stripe = stripeList.get(0);
    assertThat(stripe, notNullValue());

    assertThat("stripe1", stripe.getName(), is("stripe1"));
    List<Node> nodes = stripe.getNodes();
    assertThat(nodes.size(), is(1));
    assertThat(nodes.get(0).getName(), is("server-1"));

    ServerConfig serverConfig = nodes.get(0).getServerConfig();
    assertThat(serverConfig, notNullValue());

    TcConfig clusterTcConfig = serverConfig.getTcConfig();
    assertThat(clusterTcConfig, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();
    assertThat(tcConfig.getFailoverPriority(), notNullValue());
    assertThat(clusterTcConfig.getFailoverPriority(), notNullValue());
    assertThat(tcConfig.getTcProperties(), nullValue());
    assertThat(clusterTcConfig.getTcProperties(), nullValue());
    assertThat(tcConfig.getServers(), notNullValue());
    assertThat(clusterTcConfig.getServers(), notNullValue());
    assertThat(tcConfig.getServers().getClientReconnectWindow(), notNullValue());
    assertThat(clusterTcConfig.getServers().getClientReconnectWindow(), notNullValue());
    assertThat(clusterTcConfig.getServers()
        .getClientReconnectWindow(), is(tcConfig.getServers().getClientReconnectWindow()));

    List<Server> serverList = tcConfig.getServers().getServer();
    List<Server> serverListCluster = clusterTcConfig.getServers().getServer();
    assertThat(serverList, notNullValue());
    assertThat(serverListCluster, notNullValue());
    assertThat(serverListCluster.size(), is(serverList.size()));

    Server server = serverList.get(0);
    Server serverCluster = serverListCluster.get(0);
    assertThat(serverCluster.getHost(), is(server.getHost()));
    assertThat(serverCluster.getBind(), is(server.getBind()));

    Services clusterServices = clusterTcConfig.getPlugins();
    assertThat(clusterServices, notNullValue());
    List<Object> servicesOrConfigs = clusterServices.getConfigOrService();
    assertThat(servicesOrConfigs, notNullValue());
    assertThat(servicesOrConfigs.size(), is(3));

    servicesOrConfigs.forEach(object -> {
      assertThat((object instanceof Service || object instanceof Config), is(true));
      if (object instanceof Service) {
        Service service = (Service)object;
        Element serviceContent = service.getServiceContent();
        assertThat(serviceContent, notNullValue());
      } else {
        Config config = (Config)object;
        Element configContent = config.getConfigContent();
        assertThat(configContent, notNullValue());
      }
    });
  }

  @Test
  public void testMultiStripeSingleFileForStripe() throws Exception {
    Map<String, NomadServer> serverMap = new HashMap<>();
    Path outputFolderPath = folder.newFolder().toPath();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor);

    Path inputFilePathStripe1 = Paths.get(MigrationIT.class.getResource("/tc-config-1.xml").toURI());
    Path inputFilePathStripe2 = Paths.get(MigrationIT.class.getResource("/tc-config-2.xml").toURI());
    String inputFileLocation1 = inputFilePathStripe1.toString();
    String inputFileLocation2 = inputFilePathStripe2.toString();
    migration.processInput("testCluster", Arrays.asList(
        "stripe1" + "," + inputFileLocation1,
        "stripe2" + "," + inputFileLocation2));
    List<Path> subdirectory;
    try (Stream<Path> filePathStream = Files.walk(outputFolderPath)) {
      subdirectory = filePathStream.filter(Files::isDirectory)
          .filter(filePath -> !outputFolderPath.equals(filePath))
          .collect(Collectors.toList());
    }

    subdirectory.forEach(filePath -> {
      if (!outputFolderPath.equals(filePath)) {
        File file = filePath.toFile();
        if (file.isDirectory() && file.getParentFile().toPath().equals(outputFolderPath)) {
          String directoryName = file.getName();
          assertThat(directoryName
              , isOneOf(
                  "stripe1_testServer1", "stripe1_testServer2"
                  , "stripe2_testServer3", "stripe2_testServer4", "stripe2_testServer5"));
        }
      }
    });

    NomadServer nomadServer1 = serverMap.get("stripe1_testServer1");
    DiscoverResponse discoverResponse1 = nomadServer1.discover();
    String convertedConfigContent1 = discoverResponse1.getLatestChange().getResult();

    NomadServer nomadServer2 = serverMap.get("stripe1_testServer2");
    DiscoverResponse discoverResponse2 = nomadServer2.discover();
    String convertedConfigContent2 = discoverResponse2.getLatestChange().getResult();

    NomadServer nomadServer3 = serverMap.get("stripe2_testServer3");
    DiscoverResponse discoverResponse3 = nomadServer3.discover();
    String convertedConfigContent3 = discoverResponse3.getLatestChange().getResult();

    NomadServer nomadServer4 = serverMap.get("stripe2_testServer4");
    DiscoverResponse discoverResponse4 = nomadServer4.discover();
    String convertedConfigContent4 = discoverResponse4.getLatestChange().getResult();

    NomadServer nomadServer5 = serverMap.get("stripe2_testServer5");
    DiscoverResponse discoverResponse5 = nomadServer5.discover();
    String convertedConfigContent5 = discoverResponse5.getLatestChange().getResult();

    Map<String, String> serverNameConvertedConfigContentsMap = new HashMap<>();
    serverNameConvertedConfigContentsMap.put("stripe1_testServer1", convertedConfigContent1);
    serverNameConvertedConfigContentsMap.put("stripe1_testServer2", convertedConfigContent2);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer3", convertedConfigContent3);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer4", convertedConfigContent4);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer5", convertedConfigContent5);

    for (Map.Entry<String, String> entry : serverNameConvertedConfigContentsMap.entrySet()) {
      validateMultiStripeSingleFileForStripeInsideClusterResult(entry.getKey(), entry.getValue());
    }
  }

  @Test
  public void testMultiStripeSingleFileDuplicateServerNameForStripe() throws Exception {
    Map<String, NomadServer> serverMap = new HashMap<>();
    Path outputFolderPath = folder.newFolder().toPath();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor);

    Path inputFilePathStripe1 = Paths.get(MigrationIT.class.
        getResource("/tc-config-common-server-name-1.xml").toURI());
    Path inputFilePathStripe2 = Paths.get(MigrationIT.class.
        getResource("/tc-config-common-server-name-2.xml").toURI());
    String inputFileLocation1 = inputFilePathStripe1.toString();
    String inputFileLocation2 = inputFilePathStripe2.toString();
    migration.processInput("testCluster", Arrays.asList(
        "stripe1" + "," + inputFileLocation1,
        "stripe2" + "," + inputFileLocation2));
    List<Path> subdirectory;
    try (Stream<Path> filePathStream = Files.walk(outputFolderPath)) {
      subdirectory = filePathStream.filter(Files::isDirectory)
          .filter(filePath -> !outputFolderPath.equals(filePath))
          .collect(Collectors.toList());
    }

    subdirectory.forEach(filePath -> {
      if (!outputFolderPath.equals(filePath)) {
        File file = filePath.toFile();
        if (file.isDirectory() && file.getParentFile().toPath().equals(outputFolderPath)) {
          String directoryName = file.getName();
          assertThat(directoryName, isOneOf(
              "stripe1_testServer1",
              "stripe1_testServer2",
              "stripe2_testServer3",
              "stripe2_testServer4",
              "stripe2_testServer1"));
        }
      }
    });

    NomadServer nomadServer1 = serverMap.get("stripe1_testServer1");
    DiscoverResponse discoverResponse1 = nomadServer1.discover();
    String convertedConfigContent1 = discoverResponse1.getLatestChange().getResult();

    NomadServer nomadServer2 = serverMap.get("stripe1_testServer2");
    DiscoverResponse discoverResponse2 = nomadServer2.discover();
    String convertedConfigContent2 = discoverResponse2.getLatestChange().getResult();

    NomadServer nomadServer3 = serverMap.get("stripe2_testServer3");
    DiscoverResponse discoverResponse3 = nomadServer3.discover();
    String convertedConfigContent3 = discoverResponse3.getLatestChange().getResult();

    NomadServer nomadServer4 = serverMap.get("stripe2_testServer4");
    DiscoverResponse discoverResponse4 = nomadServer4.discover();
    String convertedConfigContent4 = discoverResponse4.getLatestChange().getResult();

    NomadServer nomadServer5 = serverMap.get("stripe2_testServer1");
    DiscoverResponse discoverResponse5 = nomadServer5.discover();
    String convertedConfigContent5 = discoverResponse5.getLatestChange().getResult();

    Map<String, String> serverNameConvertedConfigContentsMap = new HashMap<>();
    serverNameConvertedConfigContentsMap.put("stripe1_testServer1", convertedConfigContent1);
    serverNameConvertedConfigContentsMap.put("stripe1_testServer2", convertedConfigContent2);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer3", convertedConfigContent3);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer4", convertedConfigContent4);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer1", convertedConfigContent5);

    for (Map.Entry<String, String> entry : serverNameConvertedConfigContentsMap.entrySet()) {
      validateMultiStripeSingleFileForStripeWithDuplicateServerNameInsideClusterResult(entry.getKey()
          , entry.getValue());
    }
  }

  private void validateMultiStripeSingleFileForStripeInsideClusterResult(String serverName
      , String convertedConfigContent1) throws Exception{
    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent1);
    assertThat(configuration, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();

    assertThat(tcConfig.getFailoverPriority(), nullValue());
    assertThat(tcConfig.getTcProperties(), notNullValue());
    assertThat(tcConfig.getServers(), notNullValue());
    assertThat(tcConfig.getServers().getClientReconnectWindow(), notNullValue());

    assertThat(tcConfig.getServers(), notNullValue());
    List<Server> servers = tcConfig.getServers().getServer();
    assertThat(servers, notNullValue());

    if (serverName.equals("stripe1_testServer1") || serverName.equals("stripe1_testServer2")) {
      assertThat(servers.size(), is(2));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("testServer1", "testServer2"));
        validateMultiStripeSingleFileForStripeServers(server.getName(), server);
      });
    } else {
      assertThat(servers.size(), is(3));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("testServer3", "testServer4", "testServer5"));
        validateMultiStripeSingleFileForStripeServers(server.getName(), server);
      });
    }

    List<Cluster> clusterList1 = configuration.getExtendedConfiguration(Cluster.class);
    assertThat(clusterList1, notNullValue());
    assertThat(clusterList1.size(), is(1));
    Cluster cluster = clusterList1.get(0);
    assertThat(cluster, notNullValue());
    assertThat(cluster.getName(), is("testCluster"));

    List<Stripe> stripeList1 = cluster.getStripes();
    assertThat(stripeList1.size(), is(2));
    Stripe stripe1 = stripeList1.get(0);
    assertThat(stripe1, notNullValue());
    Stripe stripe2 = stripeList1.get(1);
    assertThat(stripe2, notNullValue());
    assertThat(stripe1.getName(), isOneOf("stripe1", "stripe2"));
    assertThat(stripe2.getName(), isOneOf("stripe1", "stripe2"));
    assertThat(stripe1.getName(), is(not(stripe2.getName())));

    List<Node> nodes1 = stripe1.getNodes();
    Set<String> uniqueMembers = new HashSet<>();
    ServerConfig serverConfig1;
    ServerConfig serverConfig2;
    ServerConfig serverConfig3;
    ServerConfig serverConfig4;
    ServerConfig serverConfig5;
    Map<String, ServerConfig> serverConfigMap1 = new HashMap<>();

    if (stripe1.getName().equals("stripe1")) {
      assertThat(nodes1.size(), is(2));
      assertThat(nodes1.get(0).getName(), isOneOf("testServer1", "testServer2"));
      assertThat(nodes1.get(1).getName(), isOneOf("testServer1", "testServer2"));
      uniqueMembers.add(nodes1.get(0).getName());
      uniqueMembers.add(nodes1.get(1).getName());
      serverConfig1 = nodes1.get(0).getServerConfig();
      serverConfig2 = nodes1.get(1).getServerConfig();
      serverConfigMap1.put(nodes1.get(0).getName(), serverConfig1);
      serverConfigMap1.put(nodes1.get(1).getName(), serverConfig2);
    } else {
      assertThat(nodes1.size(), is(3));
      assertThat(nodes1.get(0).getName(), isOneOf("testServer3", "testServer4", "testServer5"));
      assertThat(nodes1.get(1).getName(), isOneOf("testServer3", "testServer4", "testServer5"));
      assertThat(nodes1.get(2).getName(), isOneOf("testServer3", "testServer4", "testServer5"));
      uniqueMembers.add(nodes1.get(0).getName());
      uniqueMembers.add(nodes1.get(1).getName());
      uniqueMembers.add(nodes1.get(2).getName());
      serverConfig3 = nodes1.get(0).getServerConfig();
      serverConfig4 = nodes1.get(1).getServerConfig();
      serverConfig5 = nodes1.get(2).getServerConfig();
      serverConfigMap1.put(nodes1.get(0).getName(), serverConfig3);
      serverConfigMap1.put(nodes1.get(1).getName(), serverConfig4);
      serverConfigMap1.put(nodes1.get(2).getName(), serverConfig5);
    }

    List<Node> nodes2 = stripe2.getNodes();

    if (stripe2.getName().equals("stripe1")) {
      assertThat(nodes2.size(), is(2));
      assertThat(nodes2.get(0).getName(), isOneOf("testServer1", "testServer2"));
      assertThat(nodes2.get(1).getName(), isOneOf("testServer1", "testServer2"));
      uniqueMembers.add(nodes2.get(0).getName());
      uniqueMembers.add(nodes2.get(1).getName());
      serverConfig1 = nodes2.get(0).getServerConfig();
      serverConfig2 = nodes2.get(1).getServerConfig();
      serverConfigMap1.put(nodes2.get(0).getName(), serverConfig1);
      serverConfigMap1.put(nodes2.get(1).getName(), serverConfig2);
    } else {
      assertThat(nodes2.size(), is(3));
      assertThat(nodes2.get(0).getName(), isOneOf("testServer3", "testServer4", "testServer5"));
      assertThat(nodes2.get(1).getName(), isOneOf("testServer3", "testServer4", "testServer5"));
      assertThat(nodes2.get(2).getName(), isOneOf("testServer3", "testServer4", "testServer5"));
      uniqueMembers.add(nodes2.get(0).getName());
      uniqueMembers.add(nodes2.get(1).getName());
      uniqueMembers.add(nodes2.get(2).getName());
      serverConfig3 = nodes2.get(0).getServerConfig();
      serverConfig4 = nodes2.get(1).getServerConfig();
      serverConfig5 = nodes2.get(2).getServerConfig();
      serverConfigMap1.put(nodes2.get(0).getName(), serverConfig3);
      serverConfigMap1.put(nodes2.get(1).getName(), serverConfig4);
      serverConfigMap1.put(nodes2.get(2).getName(), serverConfig5);
    }

    assertThat(uniqueMembers.size(), is(5));

    serverConfigMap1.forEach((name, clusterServerConfig) -> {
      TcConfig clusterTcConfig1 = clusterServerConfig.getTcConfig();
      assertThat(clusterTcConfig1, notNullValue());
      assertThat(clusterTcConfig1.getFailoverPriority(), nullValue());
      assertThat(clusterTcConfig1.getTcProperties(), notNullValue());
      assertThat(clusterTcConfig1.getServers(), notNullValue());
      assertThat(clusterTcConfig1.getServers().getClientReconnectWindow(), notNullValue());
      assertThat(clusterTcConfig1.getServers(), notNullValue());
      assertThat(clusterTcConfig1.getServers().getServer(), notNullValue());

      List<Server> severList1 = clusterTcConfig1.getServers().getServer();

      if (name.equals("testServer1") || name.equals("testServer2")) {
        assertThat(severList1.size(), is(2));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("testServer1", "testServer2"));
          validateMultiStripeSingleFileForStripeServers(internalName, server);
        });
      } else {
        assertThat(severList1.size(), is(3));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("testServer3", "testServer4", "testServer5"));
          validateMultiStripeSingleFileForStripeServers(internalName, server);
        });
      }
      Services clusterServices = clusterTcConfig1.getPlugins();
      assertThat(clusterServices, notNullValue());
      List<Object> servicesOrConfigs = clusterServices.getConfigOrService();
      assertThat(servicesOrConfigs, notNullValue());
      assertThat(servicesOrConfigs.size(), is(4));

      servicesOrConfigs.forEach(object -> {
        assertThat((object instanceof Service || object instanceof Config), is(true));
        if (object instanceof Service) {
          Service service = (Service)object;
          Element serviceContent = service.getServiceContent();
          assertThat(serviceContent, notNullValue());
        } else {
          Config config = (Config)object;
          Element configContent = config.getConfigContent();
          assertThat(configContent, notNullValue());
        }
      });
    });
  }

  private void validateMultiStripeSingleFileForStripeWithDuplicateServerNameInsideClusterResult(
      String serverName, String convertedConfigContent1) throws Exception{
    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent1);
    assertThat(configuration, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();

    assertThat(tcConfig.getFailoverPriority(), nullValue());
    assertThat(tcConfig.getTcProperties(), notNullValue());
    assertThat(tcConfig.getServers(), notNullValue());
    assertThat(tcConfig.getServers().getClientReconnectWindow(), notNullValue());

    assertThat(tcConfig.getServers(), notNullValue());
    List<Server> servers = tcConfig.getServers().getServer();
    assertThat(servers, notNullValue());

    if (serverName.equals("stripe1_testServer1") || serverName.equals("stripe1_testServer2")) {
      assertThat(servers.size(), is(2));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("testServer1", "testServer2"));
        validateMultiStripeSingleFileDuplicateServerNameForStripeServers("stripe1"
            , server.getName(), server);
      });
    } else {
      assertThat(servers.size(), is(3));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("testServer3", "testServer4", "testServer1"));
        validateMultiStripeSingleFileDuplicateServerNameForStripeServers("stripe2"
            , server.getName(), server);
      });
    }

    List<Cluster> clusterList1 = configuration.getExtendedConfiguration(Cluster.class);
    assertThat(clusterList1, notNullValue());
    assertThat(clusterList1.size(), is(1));
    Cluster cluster = clusterList1.get(0);
    assertThat(cluster, notNullValue());
    assertThat(cluster.getName(), is("testCluster"));
    List<Stripe> stripeList1 = cluster.getStripes();
    assertThat(stripeList1.size(), is(2));

    Stripe stripe1 = stripeList1.get(0);
    assertThat(stripe1, notNullValue());
    Stripe stripe2 = stripeList1.get(1);
    assertThat(stripe2, notNullValue());
    assertThat(stripe1.getName(), isOneOf("stripe1", "stripe2"));
    assertThat(stripe2.getName(), isOneOf("stripe1", "stripe2"));
    assertThat(stripe1.getName(), is(not(stripe2.getName())));

    List<Node> nodes1 = stripe1.getNodes();
    Set<Pair<String, String>> uniqueMembers = new HashSet<>();
    ServerConfig serverConfig1;
    ServerConfig serverConfig2;
    ServerConfig serverConfig3;
    ServerConfig serverConfig4;
    ServerConfig serverConfig5;
    Map<Pair<String, String>, ServerConfig> serverConfigMap1 = new HashMap<>();

    if (stripe1.getName().equals("stripe1")) {
      assertThat(nodes1.size(), is(2));
      assertThat(nodes1.get(0).getName(), isOneOf("testServer1", "testServer2"));
      assertThat(nodes1.get(1).getName(), isOneOf("testServer1", "testServer2"));
      uniqueMembers.add(new Pair<>(stripe1.getName(), nodes1.get(0).getName()));
      uniqueMembers.add(new Pair<>(stripe1.getName(), nodes1.get(1).getName()));
      serverConfig1 = nodes1.get(0).getServerConfig();
      serverConfig2 = nodes1.get(1).getServerConfig();
      serverConfigMap1.put(new Pair<>(stripe1.getName(), nodes1.get(0).getName()), serverConfig1);
      serverConfigMap1.put(new Pair<>(stripe1.getName(), nodes1.get(1).getName()), serverConfig2);
    } else {
      assertThat(nodes1.size(), is(3));
      assertThat(nodes1.get(0).getName(), isOneOf("testServer3", "testServer4", "testServer1"));
      assertThat(nodes1.get(1).getName(), isOneOf("testServer3", "testServer4", "testServer1"));
      assertThat(nodes1.get(2).getName(), isOneOf("testServer3", "testServer4", "testServer1"));
      uniqueMembers.add(new Pair<>(stripe1.getName(), nodes1.get(0).getName()));
      uniqueMembers.add(new Pair<>(stripe1.getName(), nodes1.get(1).getName()));
      uniqueMembers.add(new Pair<>(stripe1.getName(), nodes1.get(2).getName()));
      serverConfig3 = nodes1.get(0).getServerConfig();
      serverConfig4 = nodes1.get(1).getServerConfig();
      serverConfig5 = nodes1.get(2).getServerConfig();
      serverConfigMap1.put(new Pair<>(stripe1.getName(), nodes1.get(0).getName()), serverConfig3);
      serverConfigMap1.put(new Pair<>(stripe1.getName(), nodes1.get(1).getName()), serverConfig4);
      serverConfigMap1.put(new Pair<>(stripe1.getName(), nodes1.get(2).getName()), serverConfig5);
    }

    List<Node> nodes2 = stripe2.getNodes();

    if (stripe2.getName().equals("stripe1")) {
      assertThat(nodes2.size(), is(2));
      assertThat(nodes2.get(0).getName(), isOneOf("testServer1", "testServer2"));
      assertThat(nodes2.get(1).getName(), isOneOf("testServer1", "testServer2"));
      uniqueMembers.add(new Pair<>(stripe2.getName(), nodes1.get(0).getName()));
      uniqueMembers.add(new Pair<>(stripe2.getName(), nodes1.get(1).getName()));
      serverConfig1 = nodes2.get(0).getServerConfig();
      serverConfig2 = nodes2.get(1).getServerConfig();
      serverConfigMap1.put(new Pair<>(stripe1.getName(), nodes2.get(0).getName()), serverConfig1);
      serverConfigMap1.put(new Pair<>(stripe1.getName(), nodes2.get(1).getName()), serverConfig2);
    } else {
      assertThat(nodes2.size(), is(3));
      assertThat(nodes2.get(0).getName(), isOneOf("testServer3", "testServer4", "testServer1"));
      assertThat(nodes2.get(1).getName(), isOneOf("testServer3", "testServer4", "testServer1"));
      assertThat(nodes2.get(2).getName(), isOneOf("testServer3", "testServer4", "testServer1"));
      uniqueMembers.add(new Pair<>(stripe2.getName(), nodes2.get(0).getName()));
      uniqueMembers.add(new Pair<>(stripe2.getName(), nodes2.get(1).getName()));
      uniqueMembers.add(new Pair<>(stripe2.getName(), nodes2.get(2).getName()));
      serverConfig3 = nodes2.get(0).getServerConfig();
      serverConfig4 = nodes2.get(1).getServerConfig();
      serverConfig5 = nodes2.get(2).getServerConfig();
      serverConfigMap1.put(new Pair<>(stripe2.getName(), nodes2.get(0).getName()), serverConfig3);
      serverConfigMap1.put(new Pair<>(stripe2.getName(), nodes2.get(1).getName()), serverConfig4);
      serverConfigMap1.put(new Pair<>(stripe2.getName(), nodes2.get(2).getName()), serverConfig5);
    }


    assertThat(uniqueMembers.size(), is(5));

    serverConfigMap1.forEach((stripeServerNamePair, clusterServerConfig) -> {
      TcConfig clusterTcConfig1 = clusterServerConfig.getTcConfig();
      assertThat(clusterTcConfig1, notNullValue());
      assertThat(clusterTcConfig1.getFailoverPriority(), nullValue());
      assertThat(clusterTcConfig1.getTcProperties(), notNullValue());
      assertThat(clusterTcConfig1.getServers(), notNullValue());
      assertThat(clusterTcConfig1.getServers().getClientReconnectWindow(), notNullValue());

      assertThat(clusterTcConfig1.getServers(), notNullValue());
      assertThat(clusterTcConfig1.getServers().getServer(), notNullValue());
      List<Server> severList1 = clusterTcConfig1.getServers().getServer();

      if (stripeServerNamePair.getOne().equals("stripe1") &&
          (stripeServerNamePair.getAnother().equals("testServer1")
           || stripeServerNamePair.getAnother().equals("testServer2"))) {

        assertThat(severList1.size(), is(2));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("testServer1", "testServer2"));
          validateMultiStripeSingleFileDuplicateServerNameForStripeServers(stripeServerNamePair.getOne()
              , internalName, server);
        });
      } else if (stripeServerNamePair.getOne().equals("stripe2") &&
                 (stripeServerNamePair.getAnother().equals("testServer3") ||
                  stripeServerNamePair.getAnother().equals("testServer4") ||
                  stripeServerNamePair.getAnother().equals("testServer1"))) {

        assertThat(severList1.size(), is(3));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("testServer3", "testServer4", "testServer1"));
          validateMultiStripeSingleFileDuplicateServerNameForStripeServers(stripeServerNamePair.getOne()
              , internalName, server);
        });
      } else {
        fail("Wrong servers in Stripes");
      }
      Services clusterServices = clusterTcConfig1.getPlugins();
      assertThat(clusterServices, notNullValue());

      List<Object> servicesOrConfigs = clusterServices.getConfigOrService();
      assertThat(servicesOrConfigs, notNullValue());
      assertThat(servicesOrConfigs.size(), is(4));

      servicesOrConfigs.forEach(object -> {
        assertThat((object instanceof Service || object instanceof Config), is(true));
        if (object instanceof Service) {
          Service service = (Service)object;
          Element serviceContent = service.getServiceContent();
          assertThat(serviceContent, notNullValue());
        } else {
          Config config = (Config)object;
          Element configContent = config.getConfigContent();
          assertThat(configContent, notNullValue());
        }
      });
    });
  }

  private void validateMultiStripeSingleFileForStripeServers(String serverName, Server server) {
    if (serverName.equals("testServer1")) {
      assertThat(server.getHost(), is("172.96.36.44"));
      assertThat(server.getLogs(), is("/export1/homes/kcleerem/server/passive/logs1"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(4164));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(4165));
    } else if (serverName.equals("testServer2")) {
      assertThat(server.getHost(), is("172.96.42.56"));
      assertThat(server.getLogs(), is("/export2/homes/kcleerem/server/passive/logs1"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9510));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9630));
    } else if (serverName.equals("testServer3")) {
      assertThat(server.getHost(), is("172.68.22.34"));
      assertThat(server.getLogs(), is("/export3/homes/kcleerem/server/passive/logs2"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(4190));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(4191));
    } else if (serverName.equals("testServer4")) {
      assertThat(server.getHost(), is("172.68.33.11"));
      assertThat(server.getLogs(), is("/export4/homes/kcleerem/server/passive/logs2"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9580));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9690));
    } else if (serverName.equals("testServer5")) {
      assertThat(server.getHost(), is("172.68.44.22"));
      assertThat(server.getLogs(), is("/export5/homes/kcleerem/server/passive/logs2"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9680));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9790));
    }
  }

  private void validateMultiStripeSingleFileDuplicateServerNameForStripeServers(String stripeName
      , String serverName, Server server) {
    if (stripeName.equals("stripe1") && serverName.equals("testServer1")) {
      assertThat(server.getHost(), is("172.96.36.44"));
      assertThat(server.getLogs(), is("/export1/homes/kcleerem/server/passive/logs1"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(4164));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(4165));
    } else if (stripeName.equals("stripe1") && serverName.equals("testServer2")) {
      assertThat(server.getHost(), is("172.96.42.56"));
      assertThat(server.getLogs(), is("/export2/homes/kcleerem/server/passive/logs1"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9510));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9630));
    } else if (stripeName.equals("stripe2") && serverName.equals("testServer3")) {
      assertThat(server.getHost(), is("172.68.22.34"));
      assertThat(server.getLogs(), is("/export3/homes/kcleerem/server/passive/logs2"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(4190));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(4191));
    } else if (stripeName.equals("stripe2") && serverName.equals("testServer4")) {
      assertThat(server.getHost(), is("172.68.33.11"));
      assertThat(server.getLogs(), is("/export4/homes/kcleerem/server/passive/logs2"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9580));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9690));
    } else if (stripeName.equals("stripe2") && serverName.equals("testServer1")) {
      assertThat(server.getHost(), is("172.68.44.22"));
      assertThat(server.getLogs(), is("/export5/homes/kcleerem/server/passive/logs2"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9680));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9790));
    } else {
      fail("Mismatched stripe-server combination");
    }
  }
}