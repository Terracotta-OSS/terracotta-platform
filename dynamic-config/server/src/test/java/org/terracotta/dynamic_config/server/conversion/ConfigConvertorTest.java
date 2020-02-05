/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.conversion;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.dynamic_config.server.conversion.exception.ErrorCode;
import org.terracotta.dynamic_config.server.conversion.exception.ErrorParamKey;
import org.terracotta.dynamic_config.server.conversion.exception.InvalidInputConfigurationContentException;
import org.terracotta.dynamic_config.server.conversion.exception.InvalidInputException;
import org.terracotta.dynamic_config.server.conversion.helper.ReflectionHelper;
import org.terracotta.dynamic_config.server.conversion.validators.ValidationWrapper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigConvertorTest {
  private static final String OFFHEAP_NS = "http://www.terracotta.org/config/offheap-resource";
  private static final String SECURITY_NS = "http://www.terrcotta.org/security";

  private ConfigConvertor convertor;

  @Before
  public void setUp() {
    RepositoryStructureBuilder processor = mock(RepositoryStructureBuilder.class);
    convertor = new ConfigConvertor(processor);
  }

  @Test
  public void testCreateServerConfigMapFunction() throws Exception {
    Node rootNodeForServer1 = mock(Node.class);
    Node clonedRootNodeForServer1 = mock(Node.class);
    Node clonedRootNodeForServer2 = mock(Node.class);
    List<String> serverList = Arrays.asList("server1", "server2");
    Map<Tuple2<Integer, String>, Node> serverNodeMap = new HashMap<>();
    int stripeId = 1;
    Path path = mock(Path.class);
    ConfigConvertor spiedconvertor = spy(convertor);

    doReturn(true).when(spiedconvertor).regularFile(path);
    doReturn(rootNodeForServer1).when(spiedconvertor).getRootNode(path);
    doReturn(serverList).when(spiedconvertor).extractServerNames(rootNodeForServer1);
    doReturn(clonedRootNodeForServer1, clonedRootNodeForServer2).when(spiedconvertor)
        .getClonedParentDocNode(rootNodeForServer1);
    spiedconvertor.createServerConfigMapFunction(serverNodeMap, stripeId, path);
    assertThat(serverNodeMap.size(), is(2));
    serverNodeMap.forEach((serverName, clonedNode) -> {
      assertThat(serverList.contains(serverName.getT2()), is(true));
      if ("server1".equals(serverName.getT2())) {
        assertThat((clonedRootNodeForServer1 == clonedNode), is(true));
      } else {
        assertThat((clonedRootNodeForServer2 == clonedNode), is(true));
      }
    });
    @SuppressWarnings("rawtypes")
    Optional<Map> stripeIdServerNameMapOpt = ReflectionHelper.getField(Map.class
        , "stripeServerNameMap", spiedconvertor);
    Map<?, ?> stripeIdServerNameMap = stripeIdServerNameMapOpt.get();
    assertThat(stripeIdServerNameMap.size(), is(1));
    stripeIdServerNameMap.forEach((stripe, servers) -> {
      assertThat(((List) servers).contains("server1"), is(true));
      assertThat(((List) servers).contains("server2"), is(true));
      assertThat(stripeId, is(stripe));
    });
    @SuppressWarnings("rawtypes")
    Optional<Map> configFileRootNodeMapOpt = ReflectionHelper.getField(Map.class, "configFileRootNodeMap", spiedconvertor);
    Map<?, ?> configFileRootNodeMap = configFileRootNodeMapOpt.get();
    assertThat(configFileRootNodeMap.size(), is(1));
    configFileRootNodeMap.forEach((filePath, rootNode) -> {
      assertThat(path, is(filePath));
      assertThat(rootNodeForServer1, is(rootNode));
    });
  }

  @Test
  public void testValidatePluginConfigurations() {
    List<Tuple2<Map<Path, Node>, ValidationWrapper>> validatorsWithParams = new ArrayList<>();
    Path path = mock(Path.class);
    Element element = mock(Element.class);
    Map<Path, Node> param = new HashMap<>();
    param.put(path, element);
    ValidationWrapper validator = mock(ValidationWrapper.class);
    Tuple2<Map<Path, Node>, ValidationWrapper> paramValidator = Tuple2.tuple2(param, validator);
    validatorsWithParams.add(paramValidator);
    doNothing().when(validator).check(param);
    convertor.validatePluginConfigurations(validatorsWithParams);
  }

  @Test(expected = InvalidInputConfigurationContentException.class)
  public void testValidatePluginConfigurationsExceptionThrown() {
    List<Tuple2<Map<Path, Node>, ValidationWrapper>> validatorsWithParams = new ArrayList<>();
    Path path = mock(Path.class);
    Element element = mock(Element.class);
    Map<Path, Node> param = new HashMap<>();
    param.put(path, element);
    ValidationWrapper validator = mock(ValidationWrapper.class);
    Tuple2<Map<Path, Node>, ValidationWrapper> paramValidator = Tuple2.tuple2(param, validator);
    validatorsWithParams.add(paramValidator);
    doThrow(new InvalidInputConfigurationContentException(ErrorCode.UNKNOWN_ERROR, "Blah")).when(validator)
        .check(param);
    convertor.validatePluginConfigurations(validatorsWithParams);
  }

  @Test
  public void testValidateAllConfigurationFilesHaveSamePluginTypes() {
    Path path1 = mock(Path.class);
    Path path2 = mock(Path.class);
    Path path3 = mock(Path.class);

    Map<String, Node> internalMap1 = new HashMap<>();
    Node offHeapNode1 = mock(Node.class);
    Node arbitraryNode1 = mock(Node.class);
    internalMap1.put(OFFHEAP_NS, offHeapNode1);
    internalMap1.put(SECURITY_NS, arbitraryNode1);

    Map<String, Node> internalMap2 = new HashMap<>();
    Node offHeapNode2 = mock(Node.class);
    Node arbitraryNode2 = mock(Node.class);
    internalMap2.put(OFFHEAP_NS, offHeapNode2);
    internalMap2.put(SECURITY_NS, arbitraryNode2);

    Map<String, Node> internalMap3 = new HashMap<>();
    Node offHeapNode3 = mock(Node.class);
    Node arbitraryNode3 = mock(Node.class);
    internalMap3.put(OFFHEAP_NS, offHeapNode3);
    internalMap3.put(SECURITY_NS, arbitraryNode3);

    Map<Path, Map<String, Node>> input = new HashMap<>();
    input.put(path1, internalMap1);
    input.put(path2, internalMap2);
    input.put(path3, internalMap3);
    convertor.validateAllConfigurationFilesHaveSamePluginTypes(input);
  }

  @Test
  public void testValidateAllConfigurationFilesHaveSamePluginTypesWithMissingPlugin() {
    Path path1 = mock(Path.class);
    Path path2 = mock(Path.class);
    Path path3 = mock(Path.class);

    Map<String, Node> internalMap1 = new HashMap<>();
    Node offHeapNode1 = mock(Node.class);
    Node arbitraryNode1 = mock(Node.class);
    internalMap1.put(OFFHEAP_NS, offHeapNode1);
    internalMap1.put(SECURITY_NS, arbitraryNode1);

    Map<String, Node> internalMap2 = new HashMap<>();
    Node offHeapNode2 = mock(Node.class);
    Node arbitraryNode2 = mock(Node.class);
    internalMap2.put(OFFHEAP_NS, offHeapNode2);
    internalMap2.put(SECURITY_NS, arbitraryNode2);

    Map<String, Node> internalMap3 = new HashMap<>();
    Node offHeapNode3 = mock(Node.class);
    internalMap3.put(OFFHEAP_NS, offHeapNode3);

    Map<Path, Map<String, Node>> input = new HashMap<>();
    input.put(path1, internalMap1);
    input.put(path2, internalMap2);
    input.put(path3, internalMap3);
    try {
      convertor.validateAllConfigurationFilesHaveSamePluginTypes(input);
      fail("Expected InvalidInputConfigurationContentException");
    } catch (InvalidInputConfigurationContentException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.MISMATCHED_SERVICE_CONFIGURATION));
    }

  }

  @Test
  public void testValidateAllConfigurationFilesHaveSamePluginTypesWithMismatchedPlugin() {
    Path path1 = mock(Path.class);
    Path path2 = mock(Path.class);
    Path path3 = mock(Path.class);

    Map<String, Node> internalMap1 = new HashMap<>();
    Node offHeapNode1 = mock(Node.class);
    Node arbitraryNode1 = mock(Node.class);
    internalMap1.put(OFFHEAP_NS, offHeapNode1);
    internalMap1.put(SECURITY_NS, arbitraryNode1);

    Map<String, Node> internalMap2 = new HashMap<>();
    Node offHeapNode2 = mock(Node.class);
    Node arbitraryNode2 = mock(Node.class);
    internalMap2.put(OFFHEAP_NS, offHeapNode2);
    internalMap2.put(SECURITY_NS, arbitraryNode2);

    Map<String, Node> internalMap3 = new HashMap<>();
    Node offHeapNode3 = mock(Node.class);
    Node arbitraryNode3 = mock(Node.class);
    internalMap3.put(OFFHEAP_NS, offHeapNode3);
    internalMap2.put("http://www.terracotta.org/something-else", arbitraryNode3);

    Map<Path, Map<String, Node>> input = new HashMap<>();
    input.put(path1, internalMap1);
    input.put(path2, internalMap2);
    input.put(path3, internalMap3);

    try {
      convertor.validateAllConfigurationFilesHaveSamePluginTypes(input);
      fail("Expected InvalidInputConfigurationContentException");
    } catch (InvalidInputConfigurationContentException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.MISMATCHED_SERVICE_CONFIGURATION));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBuildConfigurationFilePluginNodeMap() throws Exception {
    @SuppressWarnings("rawtypes")
    Optional<Map> configFileRootNodeMapOpt = ReflectionHelper.getField(Map.class, "configFileRootNodeMap", convertor);
    @SuppressWarnings("rawtypes")
    Map configFileRootNodeMap = configFileRootNodeMapOpt.get();

    Node dummyNode = mock(Node.class);
    when(dummyNode.getLocalName()).thenReturn("servers");

//---------------------------------------------------------------------------------------------------------------------

    Path firstPath = mock(Path.class);
    Node firstNode = mock(Node.class);//<tc-config>
    NodeList firstNodeList = mock(NodeList.class);

    when(firstNode.getChildNodes()).thenReturn(firstNodeList);
    when(firstNodeList.getLength()).thenReturn(3);//<plugins>, <servers> <tc-properties>

    Node firstPluginNode = mock(Node.class);
    when(firstPluginNode.getLocalName()).thenReturn(ConfigConvertor.PLUGINS_NODE_NAME);

    when(firstNodeList.item(0)).thenReturn(firstPluginNode);
    when(firstNodeList.item(1)).thenReturn(dummyNode);
    when(firstNodeList.item(2)).thenReturn(dummyNode);

    NodeList firstPluginNodeList = mock(NodeList.class);
    when(firstPluginNode.getChildNodes()).thenReturn(firstPluginNodeList);
    when(firstPluginNodeList.getLength()).thenReturn(2);//<config> and <service> nodes under <plugins>

    Node configNode11 = mock(Node.class);
    when(firstPluginNodeList.item(0)).thenReturn(configNode11);//<config>

    Node configNode12 = mock(Node.class);
    when(firstPluginNodeList.item(1)).thenReturn(configNode12);//<config>

    NodeList serviceNodeList11 = mock(NodeList.class);
    when(configNode11.getChildNodes()).thenReturn(serviceNodeList11);

    NodeList serviceNodeList12 = mock(NodeList.class);
    when(configNode12.getChildNodes()).thenReturn(serviceNodeList12);

    Node serviceNode11 = mock(Node.class);
    Node serviceNode12 = mock(Node.class);

    when(serviceNodeList11.getLength()).thenReturn(1);
    when(serviceNodeList11.item(0)).thenReturn(serviceNode11);
    when(serviceNode11.getNamespaceURI()).thenReturn(OFFHEAP_NS);

    when(serviceNodeList12.getLength()).thenReturn(1);
    when(serviceNodeList12.item(0)).thenReturn(serviceNode12);
    when(serviceNode12.getNamespaceURI()).thenReturn(SECURITY_NS);

//---------------------------------------------------------------------------------------------------------------------

    Path secondPath = mock(Path.class);
    Node secondNode = mock(Node.class);//<tc-config>
    NodeList secondNodeList = mock(NodeList.class);

    when(secondNode.getChildNodes()).thenReturn(secondNodeList);
    when(secondNodeList.getLength()).thenReturn(3);//<plugins>, <servers> <tc-properties>

    Node secondPluginNode = mock(Node.class); //<plugins>
    when(secondPluginNode.getLocalName()).thenReturn(ConfigConvertor.PLUGINS_NODE_NAME);

    when(secondNodeList.item(0)).thenReturn(dummyNode);
    when(secondNodeList.item(1)).thenReturn(dummyNode);
    when(secondNodeList.item(2)).thenReturn(secondPluginNode);

    NodeList secondPluginNodeList = mock(NodeList.class);
    when(secondPluginNode.getChildNodes()).thenReturn(secondPluginNodeList);
    when(secondPluginNodeList.getLength()).thenReturn(2); //<config> and <service> nodes under <plugins>

    Node configNode21 = mock(Node.class);
    when(secondPluginNodeList.item(0)).thenReturn(configNode21);//<config>

    Node configNode22 = mock(Node.class);
    when(secondPluginNodeList.item(1)).thenReturn(configNode22);//<config>

    NodeList serviceNodeList21 = mock(NodeList.class);
    when(configNode21.getChildNodes()).thenReturn(serviceNodeList21);

    NodeList serviceNodeList22 = mock(NodeList.class);
    when(configNode22.getChildNodes()).thenReturn(serviceNodeList22);

    Node serviceNode21 = mock(Node.class);
    Node serviceNode22 = mock(Node.class);

    when(serviceNodeList21.getLength()).thenReturn(1);
    when(serviceNodeList21.item(0)).thenReturn(serviceNode21);
    when(serviceNode21.getNamespaceURI()).thenReturn(OFFHEAP_NS);

    when(serviceNodeList22.getLength()).thenReturn(1);
    when(serviceNodeList22.item(0)).thenReturn(serviceNode22);
    when(serviceNode22.getNamespaceURI()).thenReturn(SECURITY_NS);

//---------------------------------------------------------------------------------------------------------------------

    Path thirdPath = mock(Path.class);
    Node thirdNode = mock(Node.class); //tc-config
    NodeList thirdNodeList = mock(NodeList.class);

    when(thirdNode.getChildNodes()).thenReturn(thirdNodeList);// <plugins> and <server>
    when(thirdNodeList.getLength()).thenReturn(2);

    Node thirdPluginNode = mock(Node.class); //<plugins>
    when(thirdPluginNode.getLocalName()).thenReturn(ConfigConvertor.PLUGINS_NODE_NAME);

    when(thirdNodeList.item(0)).thenReturn(dummyNode);
    when(thirdNodeList.item(1)).thenReturn(thirdPluginNode);

    NodeList thirdPluginNodeList = mock(NodeList.class);
    when(thirdPluginNode.getChildNodes()).thenReturn(thirdPluginNodeList);
    when(thirdPluginNodeList.getLength()).thenReturn(2); //<config> and <service> nodes under <plugins>

    Node configNode31 = mock(Node.class);
    when(thirdPluginNodeList.item(0)).thenReturn(configNode31); //<config>

    Node configNode32 = mock(Node.class);
    when(thirdPluginNodeList.item(1)).thenReturn(configNode32); //<config>

    NodeList serviceNodeList31 = mock(NodeList.class);
    when(configNode31.getChildNodes()).thenReturn(serviceNodeList31);

    NodeList serviceNodeList32 = mock(NodeList.class);
    when(configNode32.getChildNodes()).thenReturn(serviceNodeList32);

    Node serviceNode31 = mock(Node.class);
    Node serviceNode32 = mock(Node.class);

    when(serviceNodeList31.getLength()).thenReturn(1);
    when(serviceNodeList31.item(0)).thenReturn(serviceNode31);
    when(serviceNode31.getNamespaceURI()).thenReturn(OFFHEAP_NS);

    when(serviceNodeList32.getLength()).thenReturn(1);
    when(serviceNodeList32.item(0)).thenReturn(serviceNode32);
    when(serviceNode32.getNamespaceURI()).thenReturn(SECURITY_NS);

//---------------------------------------------------------------------------------------------------------------------

    configFileRootNodeMap.put(firstPath, firstNode);
    configFileRootNodeMap.put(secondPath, secondNode);
    configFileRootNodeMap.put(thirdPath, thirdNode);

    when(firstPluginNode.getChildNodes()).thenReturn(firstPluginNodeList);
    when(secondPluginNode.getChildNodes()).thenReturn(secondPluginNodeList);
    when(firstPluginNode.getChildNodes()).thenReturn(thirdPluginNodeList);

    Map<Path, Map<String, Node>> retMap = convertor.buildConfigurationFilePluginNodeMap();
    assertThat(3, is(retMap.size()));
    assertThat(retMap.containsKey(firstPath), is(true));
    assertThat(retMap.containsKey(secondPath), is(true));
    assertThat(retMap.containsKey(thirdPath), is(true));

    Map<String, Node> retFirstConfigMap = retMap.get(firstPath);
    Map<String, Node> retSecondConfigMap = retMap.get(secondPath);
    Map<String, Node> retThirdConfigMap = retMap.get(thirdPath);

    assertThat(retFirstConfigMap, notNullValue());
    assertThat(retSecondConfigMap, notNullValue());
    assertThat(retThirdConfigMap, notNullValue());

    assertThat(retFirstConfigMap.size(), is(2));
    assertThat(retSecondConfigMap.size(), is(2));
    assertThat(retThirdConfigMap.size(), is(2));

    assertThat(retFirstConfigMap.containsKey(OFFHEAP_NS), is(true));
    assertThat(retFirstConfigMap.containsKey(SECURITY_NS), is(true));

    assertThat(retSecondConfigMap.containsKey(OFFHEAP_NS), is(true));
    assertThat(retSecondConfigMap.containsKey(SECURITY_NS), is(true));

    assertThat(retThirdConfigMap.containsKey(OFFHEAP_NS), is(true));
    assertThat(retThirdConfigMap.containsKey(SECURITY_NS), is(true));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBuildConfigurationFilePluginNodeMapWithDuplicateEntries() throws Exception {
    @SuppressWarnings("rawtypes")
    Optional<Map> configFileRootNodeMapOpt = ReflectionHelper.getField(Map.class, "configFileRootNodeMap", convertor);
    @SuppressWarnings("rawtypes")
    Map configFileRootNodeMap = configFileRootNodeMapOpt.get();

    Node dummyNode = mock(Node.class);
    when(dummyNode.getLocalName()).thenReturn("servers");

//---------------------------------------------------------------------------------------------------------------------

    Path firstPath = mock(Path.class);
    Node firstNode = mock(Node.class);//<tc-config>
    NodeList firstNodeList = mock(NodeList.class);

    when(firstNode.getChildNodes()).thenReturn(firstNodeList);
    when(firstNodeList.getLength()).thenReturn(3);//<plugins>, <servers> <tc-properties>

    Node firstPluginNode = mock(Node.class);
    when(firstPluginNode.getLocalName()).thenReturn(ConfigConvertor.PLUGINS_NODE_NAME);

    when(firstNodeList.item(0)).thenReturn(firstPluginNode);
    when(firstNodeList.item(1)).thenReturn(dummyNode);
    when(firstNodeList.item(2)).thenReturn(dummyNode);

    NodeList firstPluginNodeList = mock(NodeList.class);
    when(firstPluginNode.getChildNodes()).thenReturn(firstPluginNodeList);
    when(firstPluginNodeList.getLength()).thenReturn(2);//<config> and <service> nodes under <plugins>

    Node configNode11 = mock(Node.class);
    when(firstPluginNodeList.item(0)).thenReturn(configNode11);//<config>

    Node configNode12 = mock(Node.class);
    when(firstPluginNodeList.item(1)).thenReturn(configNode12);//<config>

    NodeList serviceNodeList11 = mock(NodeList.class);
    when(configNode11.getChildNodes()).thenReturn(serviceNodeList11);

    NodeList serviceNodeList12 = mock(NodeList.class);
    when(configNode12.getChildNodes()).thenReturn(serviceNodeList12);

    Node serviceNode11 = mock(Node.class);
    Node serviceNode12 = mock(Node.class);

    when(serviceNodeList11.getLength()).thenReturn(1);
    when(serviceNodeList11.item(0)).thenReturn(serviceNode11);
    when(serviceNode11.getNamespaceURI()).thenReturn(OFFHEAP_NS);

    when(serviceNodeList12.getLength()).thenReturn(1);
    when(serviceNodeList12.item(0)).thenReturn(serviceNode12);
    when(serviceNode12.getNamespaceURI()).thenReturn(OFFHEAP_NS);

    configFileRootNodeMap.put(firstPath, firstNode);
    try {
      convertor.buildConfigurationFilePluginNodeMap();
      fail("Expected InvalidInputException");
    } catch (InvalidInputException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.SAME_SERVICE_DEFINED_MULTIPLE_TIMES));
    }
  }

  @Test
  public void testValidateProvidedConfigurationWithMismatchedNumbers() {
    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";
    String server4 = "server4";

    Integer stripe = 1;

    Map<Tuple2<Integer, String>, Node> hostConfigMapNode = new HashMap<>();

    hostConfigMapNode.put(Tuple2.tuple2(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server2), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server3), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server1);
    servers.add(server2);
    servers.add(server3);
    servers.add(server4);

    try {
      convertor.validateProvidedConfiguration(hostConfigMapNode, servers);
      fail("Expected InvalidInputException");
    } catch (InvalidInputException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.MISSING_SERVERS));
      assertThat(e.getParameters().containsKey(ErrorParamKey.SERVERS_IN_COMMAND.toString()), is(true));
      assertThat(e.getParameters().containsKey(ErrorParamKey.SERVERS_IN_CONFIG_FILES.toString()), is(true));
      assertThat(e.getParameters().get(ErrorParamKey.SERVERS_IN_COMMAND.toString()).size(), is(3));
      assertThat(e.getParameters().get(ErrorParamKey.SERVERS_IN_CONFIG_FILES.toString()).size(), is(4));
      assertThat(e.getParameters()
          .get(ErrorParamKey.SERVERS_IN_CONFIG_FILES.toString())
          .containsAll(servers), is(true));
      assertThat(e.getParameters()
          .get(ErrorParamKey.SERVERS_IN_COMMAND.toString())
          .containsAll(hostConfigMapNode.keySet()
              .stream()
              .map(pair -> pair.getT2())
              .collect(Collectors.toSet())), is(true));
    }
  }

  @Test
  public void testValidateProvidedConfigurationWithDifferentServers() {
    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";
    String server4 = "server4";

    Integer stripe = 1;

    Map<Tuple2<Integer, String>, Node> hostConfigMapNode = new HashMap<>();
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server2), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server3), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server1);
    servers.add(server2);
    servers.add(server4);
    try {
      convertor.validateProvidedConfiguration(hostConfigMapNode, servers);
      fail("Expected InvalidInputException");
    } catch (InvalidInputException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.MISMATCHED_SERVERS));
      assertThat(e.getParameters().containsKey(ErrorParamKey.MISMATCHED_SERVERS.toString()), is(true));
      assertThat(e.getParameters().get(ErrorParamKey.MISMATCHED_SERVERS.toString()).size(), is(1));
      assertThat(e.getParameters().get(ErrorParamKey.MISMATCHED_SERVERS.toString()).contains("server3"), is(true));
    }

  }

  @Test
  public void testValidateProvidedConfiguration() {
    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";
    String server4 = "server4";

    Integer stripe = 1;

    Map<Tuple2<Integer, String>, Node> hostConfigMapNode = new HashMap<>();
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server2), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server3), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server4), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server1);
    servers.add(server2);
    servers.add(server3);
    servers.add(server4);

    convertor.validateProvidedConfiguration(hostConfigMapNode, servers);
  }

  @Test
  public void testMismatchedServers() {
    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";

    Integer stripe = 1;

    Map<Tuple2<Integer, String>, Node> hostConfigMapNode = new HashMap<>();
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server2), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server3), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server1);
    servers.add(server2);
    servers.add(server3);

    Collection<String> mismatchedServers = convertor.mismatchedServers(hostConfigMapNode, servers);
    assertThat(mismatchedServers.size(), is(0));
  }

  @Test
  public void testMismatchedServersNotAllMatching() {
    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";
    String server4 = "server4";

    Integer stripe = 1;

    Map<Tuple2<Integer, String>, Node> hostConfigMapNode = new HashMap<>();
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(Tuple2.tuple2(stripe, server3), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server2);
    servers.add(server4);

    Collection<String> mismatchedServers = convertor.mismatchedServers(hostConfigMapNode, servers);
    assertThat(mismatchedServers.size(), is(2));
  }

  @Test
  public void testExtractServerNames() {
    Node rootNode = mock(Node.class);

    NodeList rootNodeChildren = mock(NodeList.class);
    when(rootNodeChildren.getLength()).thenReturn(3);

    when(rootNode.getChildNodes()).thenReturn(rootNodeChildren);

    Node pluginsNode = mock(Node.class);
    Node propertiesNode = mock(Node.class);
    Node serversNode = mock(Node.class);

    when(pluginsNode.getNamespaceURI()).thenReturn(ConfigConvertor.TERRACOTTA_CONFIG_NAMESPACE);
    when(propertiesNode.getNamespaceURI()).thenReturn(ConfigConvertor.TERRACOTTA_CONFIG_NAMESPACE);
    when(serversNode.getNamespaceURI()).thenReturn(ConfigConvertor.TERRACOTTA_CONFIG_NAMESPACE);

    when(pluginsNode.getLocalName()).thenReturn(ConfigConvertor.PLUGINS_NODE_NAME);
    when(propertiesNode.getLocalName()).thenReturn("tc-properties");
    when(serversNode.getLocalName()).thenReturn(ConfigConvertor.SERVERS_NODE_NAME);

    when(rootNodeChildren.item(0)).thenReturn(pluginsNode);
    when(rootNodeChildren.item(1)).thenReturn(propertiesNode);
    when(rootNodeChildren.item(2)).thenReturn(serversNode);

    NodeList serversChildrenNode = mock(NodeList.class);
    when(serversNode.getChildNodes()).thenReturn(serversChildrenNode);
    when(serversChildrenNode.getLength()).thenReturn(3);

    Node serverNode1 = mock(Node.class);
    Node serverNode2 = mock(Node.class);
    Node reconnectionWindowNode = mock(Node.class);

    when(serversChildrenNode.item(0)).thenReturn(serverNode1);
    when(serversChildrenNode.item(1)).thenReturn(serverNode2);
    when(serversChildrenNode.item(2)).thenReturn(reconnectionWindowNode);

    when(serverNode1.getNamespaceURI()).thenReturn(ConfigConvertor.TERRACOTTA_CONFIG_NAMESPACE);
    when(serverNode1.getLocalName()).thenReturn(ConfigConvertor.SERVER_NODE_NAME);

    when(serverNode2.getNamespaceURI()).thenReturn(ConfigConvertor.TERRACOTTA_CONFIG_NAMESPACE);
    when(serverNode2.getLocalName()).thenReturn(ConfigConvertor.SERVER_NODE_NAME);

    when(reconnectionWindowNode.getNamespaceURI()).thenReturn(ConfigConvertor.TERRACOTTA_CONFIG_NAMESPACE);
    when(reconnectionWindowNode.getLocalName()).thenReturn("client-reconnect-window");

    ConfigConvertor spyconvertor = spy(convertor);
    doReturn("server1").when(spyconvertor).getAttributeValue(serverNode1, ConfigConvertor.NAME_NODE_NAME);
    doReturn("server2").when(spyconvertor).getAttributeValue(serverNode2, ConfigConvertor.NAME_NODE_NAME);

    List<String> servers = spyconvertor.extractServerNames(rootNode);

    assertThat(servers.size(), is(2));
    assertThat(servers.contains("server1"), is(true));
    assertThat(servers.contains("server2"), is(true));
  }

  @Test
  public void testRemapPlatformPersistenceWithNullDataDirName() {
    Node dataRootNode = null;
    Node platformPersistenceNode = mock(Node.class);
    Path path = mock(Path.class);

    try {
      convertor.remapPlatformPersistence(dataRootNode, platformPersistenceNode, path);
      fail("Expected InvalidInputConfigurationContentException");
    } catch (InvalidInputConfigurationContentException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.NO_DATA_DIR_WITH_PLATFORM_PERSISTENCE));
      assertThat(e.getParameters().containsKey(ErrorParamKey.CONFIG_FILE.toString()), is(true));
    }
  }

  @Test
  public void testRemapPlatformPersistenceMismatchedDataRootName() {
    Node dataRootNode = mock(Node.class);
    Node platformPersistenceNode = mock(Node.class);
    Path path = mock(Path.class);
    NodeList dataRootChildList = mock(NodeList.class);
    Node platformDataRootNode = mock(Node.class);

    ConfigConvertor spyconvertor = spy(convertor);
    doReturn("data-root").when(spyconvertor).getAttributeValue(platformPersistenceNode, "data-directory-id");
    when(dataRootNode.getChildNodes()).thenReturn(dataRootChildList);
    when(dataRootChildList.getLength()).thenReturn(1);
    when(dataRootChildList.item(0)).thenReturn(platformDataRootNode);
    doReturn("anything-other-than-data-root").when(spyconvertor).getAttributeValue(platformDataRootNode, "name");
    try {
      spyconvertor.remapPlatformPersistence(dataRootNode, platformPersistenceNode, path);
      fail("Expected InvalidInputConfigurationContentException");
    } catch (InvalidInputConfigurationContentException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.INVALID_DATA_DIR_FOR_PLATFORM_PERSISTENCE));
      assertThat(e.getParameters().containsKey(ErrorParamKey.CONFIG_FILE.toString()), is(true));
    }
  }

  @Test
  public void testRemapPlatformPersistence() {
    Node dataRootNode = mock(Node.class);
    Node platformPersistenceNode = mock(Node.class);
    Path path = mock(Path.class);
    NodeList dataRootChildList = mock(NodeList.class);
    Node platformDataRootNode = mock(Node.class);

    ConfigConvertor spyconvertor = spy(convertor);

    doReturn("data-root").when(spyconvertor).
        getAttributeValue(platformPersistenceNode, "data-directory-id");
    when(dataRootNode.getChildNodes()).thenReturn(dataRootChildList);
    when(dataRootChildList.getLength()).thenReturn(1);
    when(dataRootChildList.item(0)).thenReturn(platformDataRootNode);
    doReturn("data-root").when(spyconvertor).getAttributeValue(platformDataRootNode, "name");
    doReturn(null).when(spyconvertor).
        getAttributeValue(platformDataRootNode, "use-for-platform", false);

    ArgumentCaptor<Node> platformDataRootNodeCaptor = ArgumentCaptor.forClass(Node.class);
    ArgumentCaptor<String> platformDataRootNodeAttributeNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> platformDataRootNodeAttributeValueCaptor = ArgumentCaptor.forClass(String.class);

    doNothing().when(spyconvertor).setAttributeValue(platformDataRootNodeCaptor.capture()
        , platformDataRootNodeAttributeNameCaptor.capture()
        , platformDataRootNodeAttributeValueCaptor.capture());

    ArgumentCaptor<Node> platformPersistenceNodeCaptor = ArgumentCaptor.forClass(Node.class);
    ArgumentCaptor<Boolean> removeEmptyParentCaptor = ArgumentCaptor.forClass(Boolean.class);

    doNothing().when(spyconvertor).removeNode(platformPersistenceNodeCaptor.capture(), removeEmptyParentCaptor.capture());
    spyconvertor.remapPlatformPersistence(dataRootNode, platformPersistenceNode, path);

    assertThat(platformDataRootNodeCaptor.getValue(), is(platformDataRootNode));
    assertThat(platformDataRootNodeAttributeNameCaptor.getValue(), is("use-for-platform"));
    assertThat(platformDataRootNodeAttributeValueCaptor.getValue(), is("true"));

    assertThat(platformPersistenceNodeCaptor.getValue(), is(platformPersistenceNode));
    assertThat(removeEmptyParentCaptor.getValue(), is(true));
  }

  @Test
  public void testRemapPlatformPersistenceWithUseForPlatformPresentAndFalse() {
    Node dataRootNode = mock(Node.class);
    Node platformPersistenceNode = mock(Node.class);
    Path path = mock(Path.class);
    NodeList dataRootChildList = mock(NodeList.class);
    Node platformDataRootNode = mock(Node.class);

    ConfigConvertor spyconvertor = spy(convertor);

    doReturn("data-root").when(spyconvertor).
        getAttributeValue(platformPersistenceNode, "data-directory-id");
    when(dataRootNode.getChildNodes()).thenReturn(dataRootChildList);
    when(dataRootChildList.getLength()).thenReturn(1);
    when(dataRootChildList.item(0)).thenReturn(platformDataRootNode);
    doReturn("data-root").when(spyconvertor).getAttributeValue(platformDataRootNode, "name");
    doReturn("false").when(spyconvertor).
        getAttributeValue(platformDataRootNode, "use-for-platform", false);

    ArgumentCaptor<Node> platformDataRootNodeCaptor = ArgumentCaptor.forClass(Node.class);
    ArgumentCaptor<String> platformDataRootNodeAttributeNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> platformDataRootNodeAttributeValueCaptor = ArgumentCaptor.forClass(String.class);

    doNothing().when(spyconvertor).setAttributeValue(platformDataRootNodeCaptor.capture()
        , platformDataRootNodeAttributeNameCaptor.capture()
        , platformDataRootNodeAttributeValueCaptor.capture());

    ArgumentCaptor<Node> platformPersistenceNodeCaptor = ArgumentCaptor.forClass(Node.class);
    ArgumentCaptor<Boolean> removeEmptyParentCaptor = ArgumentCaptor.forClass(Boolean.class);

    doNothing().when(spyconvertor).removeNode(platformPersistenceNodeCaptor.capture(), removeEmptyParentCaptor.capture());
    spyconvertor.remapPlatformPersistence(dataRootNode, platformPersistenceNode, path);

    assertThat(platformDataRootNodeCaptor.getValue(), is(platformDataRootNode));
    assertThat(platformDataRootNodeAttributeNameCaptor.getValue(), is("use-for-platform"));
    assertThat(platformDataRootNodeAttributeValueCaptor.getValue(), is("true"));

    assertThat(platformPersistenceNodeCaptor.getValue(), is(platformPersistenceNode));
    assertThat(removeEmptyParentCaptor.getValue(), is(true));
  }

  @Test
  public void testRemapPlatformPersistenceWithUseForPlatformPresentAndTrue() {
    Node dataRootNode = mock(Node.class);
    Node platformPersistenceNode = mock(Node.class);
    Path path = mock(Path.class);
    NodeList dataRootChildList = mock(NodeList.class);
    Node platformDataRootNode = mock(Node.class);

    ConfigConvertor spyconvertor = spy(convertor);

    doReturn("data-root").when(spyconvertor).
        getAttributeValue(platformPersistenceNode, "data-directory-id");
    when(dataRootNode.getChildNodes()).thenReturn(dataRootChildList);
    when(dataRootChildList.getLength()).thenReturn(1);
    when(dataRootChildList.item(0)).thenReturn(platformDataRootNode);
    doReturn("data-root").when(spyconvertor).getAttributeValue(platformDataRootNode, "name");
    doReturn("true").when(spyconvertor).
        getAttributeValue(platformDataRootNode, "use-for-platform", false);

    ArgumentCaptor<Node> platformPersistenceNodeCaptor = ArgumentCaptor.forClass(Node.class);
    ArgumentCaptor<Boolean> removeEmptyParentCaptor = ArgumentCaptor.forClass(Boolean.class);

    doNothing().when(spyconvertor).removeNode(platformPersistenceNodeCaptor.capture(), removeEmptyParentCaptor.capture());
    spyconvertor.remapPlatformPersistence(dataRootNode, platformPersistenceNode, path);

    verify(spyconvertor, never()).setAttributeValue(any(Node.class), anyString(), anyString());

    assertThat(platformPersistenceNodeCaptor.getValue(), is(platformPersistenceNode));
    assertThat(removeEmptyParentCaptor.getValue(), is(true));
  }
}