/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.test.util.MigrationITResultProcessor;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.ParameterSubstitutor;
import com.terracottatech.dynamic_config.xml.XmlConfigMapper;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcCluster;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcNode;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcServerConfig;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcStripe;
import com.terracottatech.migration.MigrationImpl;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.utilities.PathResolver;
import com.terracottatech.utilities.Tuple2;
import com.terracottatech.utilities.junit.TmpDir;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.terracotta.config.util.ParameterSubstitutor.substitute;

public class MigrationIT {

  @Rule
  public TmpDir tmpDir = new TmpDir();

  private XmlConfigMapper xmlConfigMapper;
  private final IParameterSubstitutor parameterSubstitutor = new ParameterSubstitutor();

  @Before
  public void setUp() {
    PathResolver userDirResolver = new PathResolver(Paths.get("%(user.dir)"), parameterSubstitutor::substitute);
    xmlConfigMapper = new XmlConfigMapper(userDirResolver);
  }

  @Test
  public void testSingleStripeSingleFile() throws Exception {
    Map<String, NomadServer<NodeContext>> serverMap = new HashMap<>();

    Path outputFolderPath = tmpDir.getRoot();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor::process);

    Path inputFilePath = Paths.get(MigrationIT.class.getResource("/migration/tc-config-single-server.xml").toURI());
    migration.processInput("testCluster", singletonList("1" + "," + inputFilePath));
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
          assertThat(directoryName, is("stripe1_node-1"));
        }
      }
    });

    NomadServer<NodeContext> nomadServer = serverMap.get("stripe1_node-1");
    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();

    // load the topology model from nomad
    NodeContext topology = discoverResponse.getLatestChange().getResult();
    // convert it back to XML the same way the FileConfigStorage is doing
    // because the following assertions are done over the parses TC Configuration
    String convertedConfigContent = xmlConfigMapper.toXml(topology);
    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent);
    assertThat(configuration, notNullValue());

    List<TcCluster> clusterList = configuration.getExtendedConfiguration(TcCluster.class);
    assertThat(clusterList, notNullValue());
    assertThat(clusterList.size(), is(1));

    TcCluster cluster = clusterList.get(0);
    assertThat(cluster, notNullValue());

    assertThat(cluster.getName(), is("testCluster"));
    List<TcStripe> stripeList = cluster.getStripes();
    assertThat(stripeList.size(), is(1));

    TcStripe stripe = stripeList.get(0);
    assertThat(stripe, notNullValue());

    List<TcNode> nodes = stripe.getNodes();
    assertThat(nodes.size(), is(1));
    assertThat(nodes.get(0).getName(), is("node-1"));

    TcServerConfig serverConfig = nodes.get(0).getServerConfig();
    assertThat(serverConfig, notNullValue());

    TcConfig clusterTcConfig = serverConfig.getTcConfig();
    assertThat(clusterTcConfig, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();
    assertThat(tcConfig.getFailoverPriority(), notNullValue());
    assertThat(clusterTcConfig.getFailoverPriority(), notNullValue());
    // tc-properties tag is always generated by Nomad marshalling of topology model
    assertThat(tcConfig.getTcProperties(), notNullValue());
    assertThat(clusterTcConfig.getTcProperties(), notNullValue());
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
  public void testSingleStripeSingleFileNoStripeId() throws Exception {
    Map<String, NomadServer<NodeContext>> serverMap = new HashMap<>();
    Path outputFolderPath = tmpDir.getRoot();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor::process);

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
          assertThat(directoryName, is("stripe1_node-1"));
        }
      }
    });

    NomadServer<NodeContext> nomadServer = serverMap.get("stripe1_node-1");
    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();

    // load the topology model from nomad
    NodeContext topology = discoverResponse.getLatestChange().getResult();
    // convert it back to XML the same way the FileConfigStorage is doing
    // because the following assertions are done over the parses TC Configuration
    String convertedConfigContent = xmlConfigMapper.toXml(topology);
    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent);
    assertThat(configuration, notNullValue());

    List<TcCluster> clusterList = configuration.getExtendedConfiguration(TcCluster.class);
    assertThat(clusterList, notNullValue());
    assertThat(clusterList.size(), is(1));

    TcCluster cluster = clusterList.get(0);
    assertThat(cluster, notNullValue());

    assertThat(cluster.getName(), is("testCluster"));
    List<TcStripe> stripeList = cluster.getStripes();
    assertThat(stripeList.size(), is(1));

    TcStripe stripe = stripeList.get(0);
    assertThat(stripe, notNullValue());

    List<TcNode> nodes = stripe.getNodes();
    assertThat(nodes.size(), is(1));
    assertThat(nodes.get(0).getName(), is("node-1"));

    TcServerConfig serverConfig = nodes.get(0).getServerConfig();
    assertThat(serverConfig, notNullValue());

    TcConfig clusterTcConfig = serverConfig.getTcConfig();
    assertThat(clusterTcConfig, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();
    assertThat(tcConfig.getFailoverPriority(), notNullValue());
    assertThat(clusterTcConfig.getFailoverPriority(), notNullValue());
    // tc-properties tag is always generated by Nomad marshalling of topology model
    assertThat(tcConfig.getTcProperties(), notNullValue());
    assertThat(clusterTcConfig.getTcProperties(), notNullValue());
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
    Map<String, NomadServer<NodeContext>> serverMap = new HashMap<>();
    Path outputFolderPath = tmpDir.getRoot();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor::process);

    Path inputFilePath = Paths.get(MigrationIT.class.getResource("/migration/tc-config-single-server-with-security.xml")
        .toURI());
    migration.processInput("testCluster", singletonList("1" + "," + inputFilePath));
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
          assertThat(directoryName, is("stripe1_node-1"));
        }
      }
    });


    NomadServer<NodeContext> nomadServer = serverMap.get("stripe1_node-1");
    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();
    // load the topology model from nomad
    NodeContext topology = discoverResponse.getLatestChange().getResult();
    // convert it back to XML the same way the FileConfigStorage is doing
    // because the following assertions are done over the parses TC Configuration
    String convertedConfigContent = xmlConfigMapper.toXml(topology);
    TcConfiguration configuration = TCConfigurationParser.parse(convertedConfigContent);
    assertThat(configuration, notNullValue());

    List<TcCluster> clusterList = configuration.getExtendedConfiguration(TcCluster.class);
    assertThat(clusterList, notNullValue());
    assertThat(clusterList.size(), is(1));

    TcCluster cluster = clusterList.get(0);
    assertThat(cluster, notNullValue());

    assertThat(cluster.getName(), is("testCluster"));
    List<TcStripe> stripeList = cluster.getStripes();
    assertThat(stripeList.size(), is(1));

    TcStripe stripe = stripeList.get(0);
    assertThat(stripe, notNullValue());

    List<TcNode> nodes = stripe.getNodes();
    assertThat(nodes.size(), is(1));
    assertThat(nodes.get(0).getName(), is("node-1"));

    TcServerConfig serverConfig = nodes.get(0).getServerConfig();
    assertThat(serverConfig, notNullValue());

    TcConfig clusterTcConfig = serverConfig.getTcConfig();
    assertThat(clusterTcConfig, notNullValue());

    TcConfig tcConfig = configuration.getPlatformConfiguration();
    assertThat(tcConfig.getFailoverPriority(), notNullValue());
    assertThat(clusterTcConfig.getFailoverPriority(), notNullValue());
    // tc-properties tag is always generated by Nomad marshalling of topology model
    assertThat(tcConfig.getTcProperties(), notNullValue());
    assertThat(clusterTcConfig.getTcProperties(), notNullValue());
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
    Map<String, NomadServer<NodeContext>> serverMap = new HashMap<>();
    Path outputFolderPath = tmpDir.getRoot();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor::process);

    Path inputFilePathStripe1 = Paths.get(MigrationIT.class.getResource("/migration/tc-config-1.xml").toURI());
    Path inputFilePathStripe2 = Paths.get(MigrationIT.class.getResource("/migration/tc-config-2.xml").toURI());
    migration.processInput("testCluster", Arrays.asList(
        "1," + inputFilePathStripe1,
        "2," + inputFilePathStripe2));

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
          assertThat(directoryName, isOneOf("stripe1_node-1", "stripe1_node-2", "stripe2_node-3", "stripe2_node-4"));
        }
      }
    });

    NomadServer<NodeContext> nomadServer1 = serverMap.get("stripe1_node-1");
    DiscoverResponse<NodeContext> discoverResponse1 = nomadServer1.discover();
    NodeContext convertedConfigContent1 = discoverResponse1.getLatestChange().getResult();

    NomadServer<NodeContext> nomadServer2 = serverMap.get("stripe1_node-2");
    DiscoverResponse<NodeContext> discoverResponse2 = nomadServer2.discover();
    NodeContext convertedConfigContent2 = discoverResponse2.getLatestChange().getResult();

    NomadServer<NodeContext> nomadServer3 = serverMap.get("stripe2_node-3");
    DiscoverResponse<NodeContext> discoverResponse3 = nomadServer3.discover();
    NodeContext convertedConfigContent3 = discoverResponse3.getLatestChange().getResult();

    NomadServer<NodeContext> nomadServer4 = serverMap.get("stripe2_node-4");
    DiscoverResponse<NodeContext> discoverResponse4 = nomadServer4.discover();
    NodeContext convertedConfigContent4 = discoverResponse4.getLatestChange().getResult();

    Map<String, NodeContext> serverNameConvertedConfigContentsMap = new HashMap<>();
    serverNameConvertedConfigContentsMap.put("stripe1_node-1", convertedConfigContent1);
    serverNameConvertedConfigContentsMap.put("stripe1_node-2", convertedConfigContent2);
    serverNameConvertedConfigContentsMap.put("stripe2_node-3", convertedConfigContent3);
    serverNameConvertedConfigContentsMap.put("stripe2_node-4", convertedConfigContent4);

    for (Map.Entry<String, NodeContext> entry : serverNameConvertedConfigContentsMap.entrySet()) {
      validateMultiStripeSingleFileForStripeInsideClusterResult(entry.getKey(), entry.getValue());
    }
  }

  @Test
  public void testMultiStripeSingleFileDuplicateServerNameForStripe() throws Exception {
    Map<String, NomadServer<NodeContext>> serverMap = new HashMap<>();
    Path outputFolderPath = tmpDir.getRoot();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(outputFolderPath, serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor::process);

    Path inputFilePathStripe1 = Paths.get(MigrationIT.class.getResource("/migration/tc-config-common-server-name-1.xml").toURI());
    Path inputFilePathStripe2 = Paths.get(MigrationIT.class.getResource("/migration/tc-config-common-server-name-2.xml").toURI());
    migration.processInput("testCluster", Arrays.asList(
        "1" + "," + inputFilePathStripe1,
        "2" + "," + inputFilePathStripe2));

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
          assertThat(directoryName, isOneOf("stripe1_node-1", "stripe1_node-2", "stripe2_node-2", "stripe2_node-1"));
        }
      }
    });

    NomadServer<NodeContext> nomadServer1 = serverMap.get("stripe1_node-1");
    DiscoverResponse<NodeContext> discoverResponse1 = nomadServer1.discover();
    NodeContext convertedConfigContent1 = discoverResponse1.getLatestChange().getResult();

    NomadServer<NodeContext> nomadServer2 = serverMap.get("stripe1_node-2");
    DiscoverResponse<NodeContext> discoverResponse2 = nomadServer2.discover();
    NodeContext convertedConfigContent2 = discoverResponse2.getLatestChange().getResult();

    NomadServer<NodeContext> nomadServer3 = serverMap.get("stripe2_node-2");
    DiscoverResponse<NodeContext> discoverResponse3 = nomadServer3.discover();
    NodeContext convertedConfigContent3 = discoverResponse3.getLatestChange().getResult();

    NomadServer<NodeContext> nomadServer5 = serverMap.get("stripe2_node-1");
    DiscoverResponse<NodeContext> discoverResponse5 = nomadServer5.discover();
    NodeContext convertedConfigContent5 = discoverResponse5.getLatestChange().getResult();

    Map<String, NodeContext> serverNameConvertedConfigContentsMap = new HashMap<>();
    serverNameConvertedConfigContentsMap.put("stripe1_node-1", convertedConfigContent1);
    serverNameConvertedConfigContentsMap.put("stripe1_node-2", convertedConfigContent2);
    serverNameConvertedConfigContentsMap.put("stripe2_node-2", convertedConfigContent3);
    serverNameConvertedConfigContentsMap.put("stripe2_node-1", convertedConfigContent5);

    for (Map.Entry<String, NodeContext> entry : serverNameConvertedConfigContentsMap.entrySet()) {
      validateMultiStripeSingleFileForStripeWithDuplicateServerNameInsideClusterResult(entry.getKey(), entry.getValue());
    }
  }

  private void validateMultiStripeSingleFileForStripeInsideClusterResult(String serverName, NodeContext topology) throws Exception {
    String convertedConfigContent1 = xmlConfigMapper.toXml(topology);
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

    if (serverName.equals("stripe1_node-1") || serverName.equals("stripe1_node-2")) {
      assertThat(servers.size(), is(2));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("node-1", "node-2"));
        validateMultiStripeSingleFileForStripeServers(server.getName(), server);
      });
    } else {
      assertThat(servers.size(), is(2));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("node-3", "node-4"));
        validateMultiStripeSingleFileForStripeServers(server.getName(), server);
      });
    }

    List<TcCluster> clusterList1 = configuration.getExtendedConfiguration(TcCluster.class);
    assertThat(clusterList1, notNullValue());
    assertThat(clusterList1.size(), is(1));
    TcCluster cluster = clusterList1.get(0);
    assertThat(cluster, notNullValue());
    assertThat(cluster.getName(), is("testCluster"));

    List<TcStripe> stripeList1 = cluster.getStripes();
    assertThat(stripeList1.size(), is(2));
    TcStripe stripe1 = stripeList1.get(0);
    assertThat(stripe1, notNullValue());
    TcStripe stripe2 = stripeList1.get(1);
    assertThat(stripe2, notNullValue());

    List<TcNode> nodes1 = stripe1.getNodes();
    Set<String> uniqueMembers = new HashSet<>();
    TcServerConfig serverConfig1;
    TcServerConfig serverConfig2;
    TcServerConfig serverConfig3;
    TcServerConfig serverConfig4;
    Map<String, TcServerConfig> serverConfigMap1 = new HashMap<>();

    assertThat(nodes1.size(), is(2));
    assertThat(nodes1.get(0).getName(), isOneOf("node-1", "node-2"));
    assertThat(nodes1.get(1).getName(), isOneOf("node-1", "node-2"));
    uniqueMembers.add(nodes1.get(0).getName());
    uniqueMembers.add(nodes1.get(1).getName());
    serverConfig1 = nodes1.get(0).getServerConfig();
    serverConfig2 = nodes1.get(1).getServerConfig();
    serverConfigMap1.put(nodes1.get(0).getName(), serverConfig1);
    serverConfigMap1.put(nodes1.get(1).getName(), serverConfig2);

    List<TcNode> nodes2 = stripe2.getNodes();
    assertThat(nodes2.size(), is(2));
    assertThat(nodes2.get(0).getName(), isOneOf("node-3", "node-4"));
    assertThat(nodes2.get(1).getName(), isOneOf("node-3", "node-4"));
    uniqueMembers.add(nodes2.get(0).getName());
    uniqueMembers.add(nodes2.get(1).getName());
    serverConfig3 = nodes2.get(0).getServerConfig();
    serverConfig4 = nodes2.get(1).getServerConfig();
    serverConfigMap1.put(nodes2.get(0).getName(), serverConfig3);
    serverConfigMap1.put(nodes2.get(1).getName(), serverConfig4);

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

      if (name.equals("node-1") || name.equals("node-2")) {
        assertThat(severList1.size(), is(2));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("node-1", "node-2"));
          validateMultiStripeSingleFileForStripeServers(internalName, server);
        });
      } else {
        assertThat(severList1.size(), is(2));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("node-3", "node-4"));
          validateMultiStripeSingleFileForStripeServers(internalName, server);
        });
      }
      Services clusterServices = clusterTcConfig1.getPlugins();
      assertThat(clusterServices, notNullValue());
      List<Object> servicesOrConfigs = clusterServices.getConfigOrService();
      assertThat(servicesOrConfigs, notNullValue());
      // we only have 2 plugins: offheap and dataroots
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
    });
  }

  private void validateMultiStripeSingleFileForStripeWithDuplicateServerNameInsideClusterResult(
      String serverName, NodeContext topology) throws Exception {
    // convert back to XML the same way the FileConfigStorage is doing
    // because the following assertions are done over the parses TC Configuration
    String convertedConfigContent1 = xmlConfigMapper.toXml(topology);
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

    if (serverName.equals("stripe1_node-1") || serverName.equals("stripe1_node-2")) {
      assertThat(servers.size(), is(2));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("node-1", "node-2"));
        validateMultiStripeSingleFileDuplicateServerNameForStripeServers(1, server.getName(), server);
      });
    } else {
      assertThat(servers.size(), is(2));
      servers.forEach(server -> {
        assertThat(server.getName(), isOneOf("node-2", "node-3", "node-1"));
        validateMultiStripeSingleFileDuplicateServerNameForStripeServers(2, server.getName(), server);
      });
    }

    List<TcCluster> clusterList1 = configuration.getExtendedConfiguration(TcCluster.class);
    assertThat(clusterList1, notNullValue());
    assertThat(clusterList1.size(), is(1));
    TcCluster cluster = clusterList1.get(0);
    assertThat(cluster, notNullValue());
    assertThat(cluster.getName(), is("testCluster"));
    List<TcStripe> stripeList1 = cluster.getStripes();
    assertThat(stripeList1.size(), is(2));

    TcStripe stripe1 = stripeList1.get(0);
    assertThat(stripe1, notNullValue());
    TcStripe stripe2 = stripeList1.get(1);
    assertThat(stripe2, notNullValue());

    List<TcNode> nodes1 = stripe1.getNodes();
    Set<Tuple2<Integer, String>> uniqueMembers = new HashSet<>();
    TcServerConfig serverConfig1;
    TcServerConfig serverConfig2;
    TcServerConfig serverConfig3;
    TcServerConfig serverConfig4;
    Map<Tuple2<Integer, String>, TcServerConfig> serverConfigMap1 = new HashMap<>();

    assertThat(nodes1.size(), is(2));
    assertThat(nodes1.get(0).getName(), isOneOf("node-1", "node-2"));
    assertThat(nodes1.get(1).getName(), isOneOf("node-1", "node-2"));
    uniqueMembers.add(Tuple2.tuple2(1, nodes1.get(0).getName()));
    uniqueMembers.add(Tuple2.tuple2(1, nodes1.get(1).getName()));
    serverConfig1 = nodes1.get(0).getServerConfig();
    serverConfig2 = nodes1.get(1).getServerConfig();
    serverConfigMap1.put(Tuple2.tuple2(1, nodes1.get(0).getName()), serverConfig1);
    serverConfigMap1.put(Tuple2.tuple2(1, nodes1.get(1).getName()), serverConfig2);

    List<TcNode> nodes2 = stripe2.getNodes();

    assertThat(nodes2.size(), is(2));
    assertThat(nodes2.get(0).getName(), isOneOf("node-2", "node-3", "node-1"));
    assertThat(nodes2.get(1).getName(), isOneOf("node-2", "node-3", "node-1"));
    uniqueMembers.add(Tuple2.tuple2(2, nodes2.get(0).getName()));
    uniqueMembers.add(Tuple2.tuple2(2, nodes2.get(1).getName()));
    serverConfig3 = nodes2.get(0).getServerConfig();
    serverConfig4 = nodes2.get(1).getServerConfig();
    serverConfigMap1.put(Tuple2.tuple2(2, nodes2.get(0).getName()), serverConfig3);
    serverConfigMap1.put(Tuple2.tuple2(2, nodes2.get(1).getName()), serverConfig4);

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

      if (stripeServerNamePair.t1 == 1 &&
          (stripeServerNamePair.getT2().equals("node-1")
              || stripeServerNamePair.getT2().equals("node-2"))) {

        assertThat(severList1.size(), is(2));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("node-1", "node-2"));
          validateMultiStripeSingleFileDuplicateServerNameForStripeServers(stripeServerNamePair.getT1(), internalName, server);
        });
      } else if (stripeServerNamePair.t1 == 2 &&
          (stripeServerNamePair.getT2().equals("node-2") || stripeServerNamePair.getT2().equals("node-1"))) {

        assertThat(severList1.size(), is(2));
        severList1.forEach(server -> {
          String internalName = server.getName();
          assertThat(internalName, isOneOf("node-2", "node-1"));
          validateMultiStripeSingleFileDuplicateServerNameForStripeServers(stripeServerNamePair.getT1(), internalName, server);
        });
      } else {
        fail("Wrong servers in Stripes");
      }
      Services clusterServices = clusterTcConfig1.getPlugins();
      assertThat(clusterServices, notNullValue());

      List<Object> servicesOrConfigs = clusterServices.getConfigOrService();
      assertThat(servicesOrConfigs, notNullValue());
      // we only have 3 plugins: offheap, dataroots and cluster
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

  private void validateMultiStripeSingleFileForStripeServers(String serverName, Server server) {
    switch (serverName) {
      case "node-1": {
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getLogs(), isOneOf(resolve("logs/stripe1/node-1")));
        BindPort bindPort = server.getTsaPort();
        assertThat(bindPort, notNullValue());
        assertThat(bindPort.getValue(), is(9410));
        BindPort groupBindPort = server.getTsaGroupPort();
        assertThat(groupBindPort, notNullValue());
        assertThat(groupBindPort.getValue(), is(9430));
        break;
      }
      case "node-2": {
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getLogs(), isOneOf(resolve("logs/stripe1/node-2")));
        BindPort bindPort = server.getTsaPort();
        assertThat(bindPort, notNullValue());
        assertThat(bindPort.getValue(), is(9510));
        BindPort groupBindPort = server.getTsaGroupPort();
        assertThat(groupBindPort, notNullValue());
        assertThat(groupBindPort.getValue(), is(9530));
        break;
      }
      case "node-3": {
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getLogs(), isOneOf(resolve("logs/stripe2/node-3")));
        BindPort bindPort = server.getTsaPort();
        assertThat(bindPort, notNullValue());
        assertThat(bindPort.getValue(), is(9610));
        BindPort groupBindPort = server.getTsaGroupPort();
        assertThat(groupBindPort, notNullValue());
        assertThat(groupBindPort.getValue(), is(9630));
        break;
      }
      case "node-4": {
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getLogs(), isOneOf(resolve("logs/stripe2/node-4")));
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

  private void validateMultiStripeSingleFileDuplicateServerNameForStripeServers(int stripeIdx, String serverName, Server server) {
    if (stripeIdx == 1 && serverName.equals("node-1")) {
      assertThat(server.getHost(), is("localhost"));
      assertThat(server.getLogs(), isOneOf(resolve("logs/stripe1/node-1")));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9410));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9430));
    } else if (stripeIdx == 1 && serverName.equals("node-2")) {
      assertThat(server.getHost(), is("localhost"));
      assertThat(server.getLogs(), isOneOf(resolve("logs/stripe1/node-2")));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9510));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9530));
    } else if (stripeIdx == 2 && serverName.equals("node-1")) {
      assertThat(server.getHost(), is("localhost"));
      assertThat(server.getLogs(), isOneOf(resolve("logs/stripe2/node-1")));
      BindPort bindPort = server.getTsaPort();
      assertThat(bindPort, notNullValue());
      assertThat(bindPort.getValue(), is(9610));
      BindPort groupBindPort = server.getTsaGroupPort();
      assertThat(groupBindPort, notNullValue());
      assertThat(groupBindPort.getValue(), is(9630));
    } else if (stripeIdx == 2 && serverName.equals("node-2")) {
      assertThat(server.getHost(), is("localhost"));
      assertThat(server.getLogs(), isOneOf(resolve("logs/stripe2/node-2")));
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

  private String[] resolve(String template) {
    String p = Paths.get("%(user.dir)", template.split("/")).toString();
    return new String[]{p, substitute(p), "%(user.dir)/" + template};
  }
}