/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.test.util.MigrationITResultProcessor;
import com.terracottatech.migration.MigrationImpl;
import com.terracottatech.migration.util.Pair;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.topology.config.xmlobjects.Cluster;
import com.terracottatech.topology.config.xmlobjects.Node;
import com.terracottatech.topology.config.xmlobjects.ServerConfig;
import com.terracottatech.topology.config.xmlobjects.Stripe;
import org.hamcrest.CoreMatchers;
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MigrationIT {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testSingleStripeSingleFile() throws Exception {
    Map<String, NomadServer> serverMap = new HashMap<>();

    Path outputFolderPath = folder.newFolder().toPath();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor);

    Path inputFilePath = Paths.get(MigrationIT.class.getResource("/migration/tc-config-single-server.xml").toURI());
    migration.processInput("testCluster", singletonList("stripe1" + "," + inputFilePath));
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
          assertThat(directoryName, is("stripe1_testServer1"));
        }
      }
    });

    NomadServer nomadServer = serverMap.get("stripe1_testServer1");
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
    assertThat(nodes.get(0).getName(), is("testServer1"));

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
    assertThat(clusterTcConfig.getServers().getClientReconnectWindow(), is(tcConfig.getServers().getClientReconnectWindow()));

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
        Service service = (Service) object;
        Element serviceContent = service.getServiceContent();
        assertThat(serviceContent, notNullValue());
      } else {
        Config config = (Config) object;
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

    Path inputFilePath = Paths.get(MigrationIT.class.getResource("/migration/tc-config-single-server.xml").toURI());
    migration.processInput("testCluster", singletonList(inputFilePath.toString()));
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
          assertThat(directoryName, is("stripe-1_testServer1"));
        }
      }
    });

    NomadServer nomadServer = serverMap.get("stripe-1_testServer1");
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
    assertThat(nodes.get(0).getName(), is("testServer1"));

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
        Service service = (Service) object;
        Element serviceContent = service.getServiceContent();
        assertThat(serviceContent, notNullValue());
      } else {
        Config config = (Config) object;
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

    Path inputFilePath = Paths.get(MigrationIT.class.getResource("/migration/tc-config-single-server-with-security.xml")
        .toURI());
    migration.processInput("testCluster", singletonList("stripe1" + "," + inputFilePath));
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
          assertThat(directoryName, is("stripe1_testServer1"));
        }
      }
    });


    NomadServer nomadServer = serverMap.get("stripe1_testServer1");
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
    assertThat(nodes.get(0).getName(), is("testServer1"));

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
        Service service = (Service) object;
        Element serviceContent = service.getServiceContent();
        assertThat(serviceContent, notNullValue());
      } else {
        Config config = (Config) object;
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

    Path inputFilePathStripe1 = Paths.get(MigrationIT.class.getResource("/migration/tc-config-1.xml").toURI());
    Path inputFilePathStripe2 = Paths.get(MigrationIT.class.getResource("/migration/tc-config-2.xml").toURI());
    migration.processInput("testCluster", Arrays.asList("stripe1" + "," + inputFilePathStripe1, "stripe2" + "," + inputFilePathStripe2));

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
          assertThat(directoryName, isOneOf("stripe1_testServer1", "stripe1_testServer2", "stripe2_testServer3", "stripe2_testServer4"));
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

    Map<String, String> serverNameConvertedConfigContentsMap = new HashMap<>();
    serverNameConvertedConfigContentsMap.put("stripe1_testServer1", convertedConfigContent1);
    serverNameConvertedConfigContentsMap.put("stripe1_testServer2", convertedConfigContent2);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer3", convertedConfigContent3);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer4", convertedConfigContent4);

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

    Path inputFilePathStripe1 = Paths.get(MigrationIT.class.getResource("/migration/tc-config-common-server-name-1.xml").toURI());
    Path inputFilePathStripe2 = Paths.get(MigrationIT.class.getResource("/migration/tc-config-common-server-name-2.xml").toURI());
    migration.processInput("testCluster", Arrays.asList("stripe1" + "," + inputFilePathStripe1, "stripe2" + "," + inputFilePathStripe2));

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
          assertThat(directoryName, isOneOf("stripe1_testServer1", "stripe1_testServer2", "stripe2_testServer2", "stripe2_testServer1"));
        }
      }
    });

    NomadServer nomadServer1 = serverMap.get("stripe1_testServer1");
    DiscoverResponse discoverResponse1 = nomadServer1.discover();
    String convertedConfigContent1 = discoverResponse1.getLatestChange().getResult();

    NomadServer nomadServer2 = serverMap.get("stripe1_testServer2");
    DiscoverResponse discoverResponse2 = nomadServer2.discover();
    String convertedConfigContent2 = discoverResponse2.getLatestChange().getResult();

    NomadServer nomadServer3 = serverMap.get("stripe2_testServer2");
    DiscoverResponse discoverResponse3 = nomadServer3.discover();
    String convertedConfigContent3 = discoverResponse3.getLatestChange().getResult();

    NomadServer nomadServer5 = serverMap.get("stripe2_testServer1");
    DiscoverResponse discoverResponse5 = nomadServer5.discover();
    String convertedConfigContent5 = discoverResponse5.getLatestChange().getResult();

    Map<String, String> serverNameConvertedConfigContentsMap = new HashMap<>();
    serverNameConvertedConfigContentsMap.put("stripe1_testServer1", convertedConfigContent1);
    serverNameConvertedConfigContentsMap.put("stripe1_testServer2", convertedConfigContent2);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer2", convertedConfigContent3);
    serverNameConvertedConfigContentsMap.put("stripe2_testServer1", convertedConfigContent5);

    for (Map.Entry<String, String> entry : serverNameConvertedConfigContentsMap.entrySet()) {
      validateMultiStripeSingleFileForStripeWithDuplicateServerNameInsideClusterResult(entry.getKey(), entry.getValue());
    }
  }

  private void validateMultiStripeSingleFileForStripeInsideClusterResult(String serverName, String convertedConfigContent1) throws Exception {
    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent1);
    assertThat(configuration, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();

    assertThat(tcConfig.getFailoverPriority(), notNullValue());
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
      assertThat(servers.size(), is(2));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("testServer3", "testServer4"));
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
    assertThat(stripe1.getName(), is(CoreMatchers.not(stripe2.getName())));

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
      assertThat(nodes1.get(0).getName(), isOneOf("testServer3", "testServer4"));
      assertThat(nodes1.get(1).getName(), isOneOf("testServer3", "testServer4"));
      assertThat(nodes1.get(2).getName(), isOneOf("testServer3", "testServer4"));
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
      assertThat(nodes2.size(), is(2));
      assertThat(nodes2.get(0).getName(), isOneOf("testServer3", "testServer4"));
      assertThat(nodes2.get(1).getName(), isOneOf("testServer3", "testServer4"));
      uniqueMembers.add(nodes2.get(0).getName());
      uniqueMembers.add(nodes2.get(1).getName());
      serverConfig3 = nodes2.get(0).getServerConfig();
      serverConfig4 = nodes2.get(1).getServerConfig();
      serverConfigMap1.put(nodes2.get(0).getName(), serverConfig3);
      serverConfigMap1.put(nodes2.get(1).getName(), serverConfig4);
    }

    assertThat(uniqueMembers.size(), is(4));

    serverConfigMap1.forEach((name, clusterServerConfig) -> {
      TcConfig clusterTcConfig1 = clusterServerConfig.getTcConfig();
      assertThat(clusterTcConfig1, notNullValue());
      assertThat(clusterTcConfig1.getFailoverPriority(), notNullValue());
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
        assertThat(severList1.size(), is(2));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("testServer3", "testServer4"));
          validateMultiStripeSingleFileForStripeServers(internalName, server);
        });
      }
      Services clusterServices = clusterTcConfig1.getPlugins();
      assertThat(clusterServices, notNullValue());
      List<Object> servicesOrConfigs = clusterServices.getConfigOrService();
      assertThat(servicesOrConfigs, notNullValue());
      assertThat(servicesOrConfigs.size(), is(3));

      servicesOrConfigs.forEach(object -> {
        assertThat((object instanceof Service || object instanceof Config), is(true));
        if (object instanceof Service) {
          Service service = (Service) object;
          Element serviceContent = service.getServiceContent();
          assertThat(serviceContent, notNullValue());
        } else {
          Config config = (Config) object;
          Element configContent = config.getConfigContent();
          assertThat(configContent, notNullValue());
        }
      });
    });
  }

  private void validateMultiStripeSingleFileForStripeWithDuplicateServerNameInsideClusterResult(
      String serverName, String convertedConfigContent1) throws Exception {
    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent1);
    assertThat(configuration, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();

    assertThat(tcConfig.getFailoverPriority(), notNullValue());
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
        validateMultiStripeSingleFileDuplicateServerNameForStripeServers("stripe1", server.getName(), server);
      });
    } else {
      assertThat(servers.size(), is(2));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("testServer2", "testServer3", "testServer1"));
        validateMultiStripeSingleFileDuplicateServerNameForStripeServers("stripe2", server.getName(), server);
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
    assertThat(stripe1.getName(), is(CoreMatchers.not(stripe2.getName())));

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
      assertThat(nodes1.get(0).getName(), isOneOf("testServer2", "testServer3", "testServer1"));
      assertThat(nodes1.get(1).getName(), isOneOf("testServer2", "testServer3", "testServer1"));
      assertThat(nodes1.get(2).getName(), isOneOf("testServer2", "testServer3", "testServer1"));
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
      assertThat(nodes2.size(), is(2));
      assertThat(nodes2.get(0).getName(), isOneOf("testServer2", "testServer3", "testServer1"));
      assertThat(nodes2.get(1).getName(), isOneOf("testServer2", "testServer3", "testServer1"));
      uniqueMembers.add(new Pair<>(stripe2.getName(), nodes2.get(0).getName()));
      uniqueMembers.add(new Pair<>(stripe2.getName(), nodes2.get(1).getName()));
      serverConfig3 = nodes2.get(0).getServerConfig();
      serverConfig4 = nodes2.get(1).getServerConfig();
      serverConfigMap1.put(new Pair<>(stripe2.getName(), nodes2.get(0).getName()), serverConfig3);
      serverConfigMap1.put(new Pair<>(stripe2.getName(), nodes2.get(1).getName()), serverConfig4);
    }

    assertThat(uniqueMembers.size(), is(4));

    serverConfigMap1.forEach((stripeServerNamePair, clusterServerConfig) -> {
      TcConfig clusterTcConfig1 = clusterServerConfig.getTcConfig();
      assertThat(clusterTcConfig1, notNullValue());
      assertThat(clusterTcConfig1.getFailoverPriority(), notNullValue());
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
          validateMultiStripeSingleFileDuplicateServerNameForStripeServers(stripeServerNamePair.getOne(), internalName, server);
        });
      } else if (stripeServerNamePair.getOne().equals("stripe2") &&
          (stripeServerNamePair.getAnother().equals("testServer2") || stripeServerNamePair.getAnother().equals("testServer1"))) {

        assertThat(severList1.size(), is(2));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("testServer2", "testServer1"));
          validateMultiStripeSingleFileDuplicateServerNameForStripeServers(stripeServerNamePair.getOne(), internalName, server);
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
          Service service = (Service) object;
          Element serviceContent = service.getServiceContent();
          assertThat(serviceContent, notNullValue());
        } else {
          Config config = (Config) object;
          Element configContent = config.getConfigContent();
          assertThat(configContent, notNullValue());
        }
      });
    });
  }

  private void validateMultiStripeSingleFileForStripeServers(String serverName, Server server) {
    switch (serverName) {
      case "testServer1": {
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getLogs(), is("logs1"));
        BindPort bindPort = server.getTsaPort();
        assertThat(bindPort, notNullValue());
        assertThat(bindPort.getValue(), is(9410));
        BindPort groupBindPort = server.getTsaGroupPort();
        assertThat(groupBindPort, notNullValue());
        assertThat(groupBindPort.getValue(), is(9430));
        break;
      }
      case "testServer2": {
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getLogs(), is("logs2"));
        BindPort bindPort = server.getTsaPort();
        assertThat(bindPort, notNullValue());
        assertThat(bindPort.getValue(), is(9510));
        BindPort groupBindPort = server.getTsaGroupPort();
        assertThat(groupBindPort, notNullValue());
        assertThat(groupBindPort.getValue(), is(9530));
        break;
      }
      case "testServer3": {
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getLogs(), is("logs3"));
        BindPort bindPort = server.getTsaPort();
        assertThat(bindPort, notNullValue());
        assertThat(bindPort.getValue(), is(9610));
        BindPort groupBindPort = server.getTsaGroupPort();
        assertThat(groupBindPort, notNullValue());
        assertThat(groupBindPort.getValue(), is(9630));
        break;
      }
      case "testServer4": {
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getLogs(), is("logs4"));
        BindPort bindPort = server.getTsaPort();
        assertThat(bindPort, notNullValue());
        assertThat(bindPort.getValue(), is(9710));
        BindPort groupBindPort = server.getTsaGroupPort();
        assertThat(groupBindPort, notNullValue());
        assertThat(groupBindPort.getValue(), is(9730));
        break;
      }
    }
  }

  private void validateMultiStripeSingleFileDuplicateServerNameForStripeServers(String stripeName, String serverName, Server server) {
    if (stripeName.equals("stripe1") && serverName.equals("testServer1")) {
      assertThat(server.getHost(), is("localhost"));
      assertThat(server.getLogs(), is("logs1"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9410));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9430));
    } else if (stripeName.equals("stripe1") && serverName.equals("testServer2")) {
      assertThat(server.getHost(), is("localhost"));
      assertThat(server.getLogs(), is("logs2"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9510));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9530));
    } else if (stripeName.equals("stripe2") && serverName.equals("testServer1")) {
      assertThat(server.getHost(), is("localhost"));
      assertThat(server.getLogs(), is("logs1"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9610));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9630));
    } else if (stripeName.equals("stripe2") && serverName.equals("testServer2")) {
      assertThat(server.getHost(), is("localhost"));
      assertThat(server.getLogs(), is("logs2"));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9710));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9730));
    } else {
      fail("Mismatched stripe-server combination");
    }
  }
}