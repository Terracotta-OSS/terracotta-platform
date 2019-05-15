/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.terracottatech.migration.exception.ErrorCode;
import com.terracottatech.migration.exception.ErrorParamKey;
import com.terracottatech.migration.exception.InvalidInputConfigurationContentException;
import com.terracottatech.migration.exception.InvalidInputException;
import com.terracottatech.migration.helper.ReflectionHelper;
import com.terracottatech.migration.util.Pair;
import com.terracottatech.migration.validators.ValidationWrapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MigrationTest {

  @Test
  public void testValidateAndProcessInputStripeNameConfigFilePathAreGiven() throws Exception {
    MigrationImpl migration = new MigrationImpl();
    List<String> commands = Arrays.asList("stripe1,/opt/repository/tc-config-1.xml",
        "stripe2,/opt/repository/tc-config-2.xml",
        "stripe3,/opt/repository/tc-config-3.xml");
    Set<String> stripeNames = new HashSet<>(Arrays.asList("stripe1", "stripe2", "stripe3"));
    migration.validateAndProcessInput(commands);
    @SuppressWarnings("rawtypes")
    Optional<List> oneCommaCommandList = ReflectionHelper.getDeclaredField(List.class, "inputParamList", migration);

    if (oneCommaCommandList.isPresent()) {
      @SuppressWarnings("rawtypes")
      List retList = oneCommaCommandList.get();
      assertThat(retList.size(), is(commands.size()));
      for (Object obj : retList) {
        String[] commandArray = (String[])obj;
        assertThat(stripeNames.contains(commandArray[0]), is(true));
      }
    } else {
      fail("Unexpected!!! oneCommaCommands is null");
    }
  }

  @Test
  public void testValidateAndProcessInputConfigFilePathAreGiven() throws Exception {
    MigrationImpl migration = new MigrationImpl();
    List<String> commands = Arrays.asList("/opt/repository/tc-config-1.xml",
        "/opt/repository/tc-config-2.xml",
        "/opt/repository/tc-config-3.xml");
    Set<String> stripeNames = new HashSet<>(Arrays.asList("stripe-1", "stripe-2", "stripe-3"));
    migration.validateAndProcessInput(commands);
    @SuppressWarnings("rawtypes")
    Optional<List> oneCommaCommandList = ReflectionHelper.getDeclaredField(List.class, "inputParamList", migration);
    if (oneCommaCommandList.isPresent()) {
      @SuppressWarnings("rawtypes")
      List retList = oneCommaCommandList.get();
      assertThat(retList.size(), is(commands.size()));
      for (Object obj : retList) {
        String[] commandArray = (String[])obj;
        assertThat(stripeNames.contains(commandArray[0]), is(true));
      }
    } else {
      fail("Unexpected!!! oneCommaCommands is null");
    }
  }

  @Test
  public void testValidateAndProcessInputConfigFileWithMixedInput() throws Exception {
    MigrationImpl migration = new MigrationImpl();
    List<String> commands = Arrays.asList("/opt/repository/tc-config-1.xml",
        "stripe2,/opt/repository/tc-config-2.xml",
        "/opt/repository/tc-config-3.xml");
    try {
      migration.validateAndProcessInput(commands);
      fail("Expected InvalidInputException");
    } catch (InvalidInputException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.INVALID_MIXED_INPUT_PATTERN));
    }
  }

  @Test
  public void testValidateAndProcessWithDuplicateOneCommaEntries() {
    MigrationImpl migration = new MigrationImpl();
    List<String> commands = Arrays.asList("stripe1,/opt/repository/tc-config-1.xml",
        "stripe2,/opt/repository/tc-config-2.xml",
        "stripe1,/opt/repository/tc-config-3.xml");
    try {
      migration.validateAndProcessInput(commands);
      fail("Expected InvalidInputException");
    } catch (InvalidInputException e) {
      assertThat(ErrorCode.DUPLICATE_STRIPE_NAME, is(e.getErrorCode()));
      assertThat(e.getParameters().containsKey(ErrorParamKey.STRIPE_NAME.toString()), is(true));
      assertThat(e.getParameters().get(ErrorParamKey.STRIPE_NAME.toString()).size(), is(1));
      assertThat(e.getParameters().get(ErrorParamKey.STRIPE_NAME.toString()).contains("stripe1"), is(true));
    }
  }

  @Test
  public void testValidateAndProcessWithInvalidBasicEntries() {
    MigrationImpl migration = new MigrationImpl();
    List<String> commands = Arrays.asList("stripe1,/opt/repository/tc-config-2.xml,Hello",
        "stripe2,/opt/repository/tc-config-2.xml",
        "stripe1,/opt/repository/tc-config-3.xml");
    try {
      migration.validateAndProcessInput(commands);
      fail("Expected InvalidInputException");
    } catch (InvalidInputException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.INVALID_INPUT_PATTERN));
    }
  }

  @Test
  public void testValidateAndProcessWithInvalidBasicEntriesMoreNumberOfEntries() throws Exception {
    MigrationImpl migration = new MigrationImpl();
    List<String> commands = Arrays.asList("stripe1, server1, /opt/repository/tc-config-1.xml, invalid",
        "stripe2,/opt/repository/tc-config-2.xml",
        "stripe1,/opt/repository/tc-config-3.xml");
    try {
      migration.validateAndProcessInput(commands);
      fail("Expected InvalidInputException");
    } catch (InvalidInputException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.INVALID_INPUT_PATTERN));
    }
  }

  @Test
  public void testCreateServerConfigMapFunction() throws Exception {
    MigrationImpl migration = new MigrationImpl();
    Node rootNodeForServer1 = mock(Node.class);
    Node clonedRootNodeForServer1 = mock(Node.class);
    Node clonedRootNodeForServer2 = mock(Node.class);
    List<String> serverList = Arrays.asList("server1", "server2");
    Map<Pair<String, String>, Node> serverNodeMap = new HashMap<>();
    String stripeName = "Stripe1";
    Path path = mock(Path.class);
    MigrationImpl spiedMigration = spy(migration);

    doReturn(true).when(spiedMigration).regularFile(path);
    doReturn(rootNodeForServer1).when(spiedMigration).getRootNode(path);
    doReturn(serverList).when(spiedMigration).extractServerNames(rootNodeForServer1);
    doReturn(clonedRootNodeForServer1, clonedRootNodeForServer2).when(spiedMigration)
        .getClonedParentDocNode(rootNodeForServer1);
    spiedMigration.createServerConfigMapFunction(serverNodeMap, stripeName, path);
    assertThat(serverNodeMap.size(), is(2));
    serverNodeMap.forEach((serverName, clonedNode) -> {
      assertThat(serverList.contains(serverName.getAnother()), is(true));
      if ("server1".equals(serverName.getAnother())) {
        assertThat((clonedRootNodeForServer1 == clonedNode), is(true));
      } else {
        assertThat((clonedRootNodeForServer2 == clonedNode), is(true));
      }
    });
    @SuppressWarnings("rawtypes")
    Optional<Map> stripeNameServerNameMapOpt = ReflectionHelper.getField(Map.class
        , "stripeServerNameMap", spiedMigration);
    Map<?, ?> stripeNameServerNameMap = stripeNameServerNameMapOpt.get();
    assertThat(stripeNameServerNameMap.size(), is(1));
    stripeNameServerNameMap.forEach((stripe, servers) -> {
      assertThat(((List)servers).contains("server1"), is(true));
      assertThat(((List)servers).contains("server2"), is(true));
      assertThat(stripeName, is(stripe));
    });
    @SuppressWarnings("rawtypes")
    Optional<Map> configFileRootNodeMapOpt = ReflectionHelper.getField(Map.class
        , "configFileRootNodeMap", spiedMigration);
    Map<?, ?> configFileRootNodeMap = configFileRootNodeMapOpt.get();
    assertThat(configFileRootNodeMap.size(), is(1));
    configFileRootNodeMap.forEach((filePath, rootNode) -> {
      assertThat(path, is(filePath));
      assertThat(rootNodeForServer1, is(rootNode));
    });
  }

  @Test
  public void testValidatePluginConfigurations() {
    List<Pair<Map<Path, Node>, ValidationWrapper>> validatorsWithParams = new ArrayList<>();
    MigrationImpl migration = new MigrationImpl();
    Path path = mock(Path.class);
    Element element = mock(Element.class);
    Map<Path, Node> param = new HashMap<>();
    param.put(path, element);
    ValidationWrapper validator = mock(ValidationWrapper.class);
    Pair<Map<Path, Node>, ValidationWrapper> paramValidator = new Pair<>(param, validator);
    validatorsWithParams.add(paramValidator);
    doNothing().when(validator).check(param);
    migration.validatePluginConfigurations(validatorsWithParams);
  }

  @Test(expected = InvalidInputConfigurationContentException.class)
  public void testValidatePluginConfigurationsExceptionThrown() {
    List<Pair<Map<Path, Node>, ValidationWrapper>> validatorsWithParams = new ArrayList<>();
    MigrationImpl migration = new MigrationImpl();
    Path path = mock(Path.class);
    Element element = mock(Element.class);
    Map<Path, Node> param = new HashMap<>();
    param.put(path, element);
    ValidationWrapper validator = mock(ValidationWrapper.class);
    Pair<Map<Path, Node>, ValidationWrapper> paramValidator = new Pair<>(param, validator);
    validatorsWithParams.add(paramValidator);
    doThrow(new InvalidInputConfigurationContentException(ErrorCode.UNKNOWN_ERROR, "Blah")).when(validator)
        .check(param);
    migration.validatePluginConfigurations(validatorsWithParams);
  }

  @Test
  public void testValidateAllConfigurationFilesHaveSamePluginTypes() {
    MigrationImpl migration = new MigrationImpl();
    Set<String> namespaces = new HashSet<>();
    String offHeapNamespace = "http://www.terracotta.org/config/offheap-resource";
    String securityNamespace = "http://www.terrcotta.com/security";
    namespaces.add(offHeapNamespace);
    namespaces.add(securityNamespace);

    Path path1 = mock(Path.class);
    Path path2 = mock(Path.class);
    Path path3 = mock(Path.class);

    Map<String, Node> internalMap1 = new HashMap<>();
    Node offHeapNode1 = mock(Node.class);
    Node arbitraryNode1 = mock(Node.class);
    internalMap1.put(offHeapNamespace, offHeapNode1);
    internalMap1.put(securityNamespace, arbitraryNode1);

    Map<String, Node> internalMap2 = new HashMap<>();
    Node offHeapNode2 = mock(Node.class);
    Node arbitraryNode2 = mock(Node.class);
    internalMap2.put(offHeapNamespace, offHeapNode2);
    internalMap2.put(securityNamespace, arbitraryNode2);

    Map<String, Node> internalMap3 = new HashMap<>();
    Node offHeapNode3 = mock(Node.class);
    Node arbitraryNode3 = mock(Node.class);
    internalMap3.put(offHeapNamespace, offHeapNode3);
    internalMap3.put(securityNamespace, arbitraryNode3);

    Map<Path, Map<String, Node>> input = new HashMap<>();
    input.put(path1, internalMap1);
    input.put(path2, internalMap2);
    input.put(path3, internalMap3);
    migration.validateAllConfigurationFilesHaveSamePluginTypes(input);
  }

  @Test
  public void testValidateAllConfigurationFilesHaveSamePluginTypesWithMissingPlugin() {
    MigrationImpl migration = new MigrationImpl();
    Set<String> namespaces = new HashSet<>();
    String offHeapNamespace = "http://www.terracotta.org/config/offheap-resource";
    String securityNamespace = "http://www.terrcotta.com/security";
    namespaces.add(offHeapNamespace);
    namespaces.add(securityNamespace);

    Path path1 = mock(Path.class);
    Path path2 = mock(Path.class);
    Path path3 = mock(Path.class);

    Map<String, Node> internalMap1 = new HashMap<>();
    Node offHeapNode1 = mock(Node.class);
    Node arbitraryNode1 = mock(Node.class);
    internalMap1.put(offHeapNamespace, offHeapNode1);
    internalMap1.put(securityNamespace, arbitraryNode1);

    Map<String, Node> internalMap2 = new HashMap<>();
    Node offHeapNode2 = mock(Node.class);
    Node arbitraryNode2 = mock(Node.class);
    internalMap2.put(offHeapNamespace, offHeapNode2);
    internalMap2.put(securityNamespace, arbitraryNode2);

    Map<String, Node> internalMap3 = new HashMap<>();
    Node offHeapNode3 = mock(Node.class);
    internalMap3.put(offHeapNamespace, offHeapNode3);

    Map<Path, Map<String, Node>> input = new HashMap<>();
    input.put(path1, internalMap1);
    input.put(path2, internalMap2);
    input.put(path3, internalMap3);
    try {
      migration.validateAllConfigurationFilesHaveSamePluginTypes(input);
      fail("Expected InvalidInputConfigurationContentException");
    } catch (InvalidInputConfigurationContentException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.MISMATCHED_SERVICE_CONFIGURATION));
    }

  }

  @Test
  public void testValidateAllConfigurationFilesHaveSamePluginTypesWithMismatchedPlugin() {
    MigrationImpl migration = new MigrationImpl();
    Set<String> namespaces = new HashSet<>();
    String offHeapNamespace = "http://www.terracotta.org/config/offheap-resource";
    String securityNamespace = "http://www.terrcotta.org/security";
    namespaces.add(offHeapNamespace);
    namespaces.add(securityNamespace);

    Path path1 = mock(Path.class);
    Path path2 = mock(Path.class);
    Path path3 = mock(Path.class);

    Map<String, Node> internalMap1 = new HashMap<>();
    Node offHeapNode1 = mock(Node.class);
    Node arbitraryNode1 = mock(Node.class);
    internalMap1.put(offHeapNamespace, offHeapNode1);
    internalMap1.put(securityNamespace, arbitraryNode1);

    Map<String, Node> internalMap2 = new HashMap<>();
    Node offHeapNode2 = mock(Node.class);
    Node arbitraryNode2 = mock(Node.class);
    internalMap2.put(offHeapNamespace, offHeapNode2);
    internalMap2.put(securityNamespace, arbitraryNode2);

    Map<String, Node> internalMap3 = new HashMap<>();
    Node offHeapNode3 = mock(Node.class);
    Node arbitraryNode3 = mock(Node.class);
    internalMap3.put(offHeapNamespace, offHeapNode3);
    internalMap2.put("http://www.terracotta.org/something-else", arbitraryNode3);

    Map<Path, Map<String, Node>> input = new HashMap<>();
    input.put(path1, internalMap1);
    input.put(path2, internalMap2);
    input.put(path3, internalMap3);

    try {
      migration.validateAllConfigurationFilesHaveSamePluginTypes(input);
      fail("Expected InvalidInputConfigurationContentException");
    } catch (InvalidInputConfigurationContentException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.MISMATCHED_SERVICE_CONFIGURATION));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBuildConfigurationFilePluginNodeMap() throws Exception {
    MigrationImpl migration = new MigrationImpl();

    String nameSpace1 = "http://www.terracotta.org/config/off-heap";
    String nameSpace2 = "http://www.terracotta.org/config/security";

    @SuppressWarnings("rawtypes")
    Optional<Map> configFileRootNodeMapOpt = ReflectionHelper.getField(Map.class
        , "configFileRootNodeMap", migration);
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
    when(firstPluginNode.getLocalName()).thenReturn(MigrationImpl.PLUGINS_NODE_NAME);

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
    when(serviceNode11.getNamespaceURI()).thenReturn(nameSpace1);

    when(serviceNodeList12.getLength()).thenReturn(1);
    when(serviceNodeList12.item(0)).thenReturn(serviceNode12);
    when(serviceNode12.getNamespaceURI()).thenReturn(nameSpace2);

//---------------------------------------------------------------------------------------------------------------------

    Path secondPath = mock(Path.class);
    Node secondNode = mock(Node.class);//<tc-config>
    NodeList secondNodeList = mock(NodeList.class);

    when(secondNode.getChildNodes()).thenReturn(secondNodeList);
    when(secondNodeList.getLength()).thenReturn(3);//<plugins>, <servers> <tc-properties>

    Node secondPluginNode = mock(Node.class); //<plugins>
    when(secondPluginNode.getLocalName()).thenReturn(MigrationImpl.PLUGINS_NODE_NAME);

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
    when(serviceNode21.getNamespaceURI()).thenReturn(nameSpace1);

    when(serviceNodeList22.getLength()).thenReturn(1);
    when(serviceNodeList22.item(0)).thenReturn(serviceNode22);
    when(serviceNode22.getNamespaceURI()).thenReturn(nameSpace2);

//---------------------------------------------------------------------------------------------------------------------

    Path thirdPath = mock(Path.class);
    Node thirdNode = mock(Node.class); //tc-config
    NodeList thirdNodeList = mock(NodeList.class);

    when(thirdNode.getChildNodes()).thenReturn(thirdNodeList);// <plugins> and <server>
    when(thirdNodeList.getLength()).thenReturn(2);

    Node thirdPluginNode = mock(Node.class); //<plugins>
    when(thirdPluginNode.getLocalName()).thenReturn(MigrationImpl.PLUGINS_NODE_NAME);

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
    when(serviceNode31.getNamespaceURI()).thenReturn(nameSpace1);

    when(serviceNodeList32.getLength()).thenReturn(1);
    when(serviceNodeList32.item(0)).thenReturn(serviceNode32);
    when(serviceNode32.getNamespaceURI()).thenReturn(nameSpace2);

//---------------------------------------------------------------------------------------------------------------------

    configFileRootNodeMap.put(firstPath, firstNode);
    configFileRootNodeMap.put(secondPath, secondNode);
    configFileRootNodeMap.put(thirdPath, thirdNode);

    when(firstPluginNode.getChildNodes()).thenReturn(firstPluginNodeList);
    when(secondPluginNode.getChildNodes()).thenReturn(secondPluginNodeList);
    when(firstPluginNode.getChildNodes()).thenReturn(thirdPluginNodeList);

    Map<Path, Map<String, Node>> retMap = migration.buildConfigurationFilePluginNodeMap();
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

    assertThat(retFirstConfigMap.containsKey(nameSpace1), is(true));
    assertThat(retFirstConfigMap.containsKey(nameSpace2), is(true));

    assertThat(retSecondConfigMap.containsKey(nameSpace1), is(true));
    assertThat(retSecondConfigMap.containsKey(nameSpace2), is(true));

    assertThat(retThirdConfigMap.containsKey(nameSpace1), is(true));
    assertThat(retThirdConfigMap.containsKey(nameSpace2), is(true));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBuildConfigurationFilePluginNodeMapWithDuplicateEntries() throws Exception {
    MigrationImpl migration = new MigrationImpl();
    String nameSpace1 = "http://www.terracotta.org/config/off-heap";
    @SuppressWarnings("rawtypes")
    Optional<Map> configFileRootNodeMapOpt = ReflectionHelper.getField(Map.class
        , "configFileRootNodeMap", migration);
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
    when(firstPluginNode.getLocalName()).thenReturn(MigrationImpl.PLUGINS_NODE_NAME);

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
    when(serviceNode11.getNamespaceURI()).thenReturn(nameSpace1);

    when(serviceNodeList12.getLength()).thenReturn(1);
    when(serviceNodeList12.item(0)).thenReturn(serviceNode12);
    when(serviceNode12.getNamespaceURI()).thenReturn(nameSpace1);

    configFileRootNodeMap.put(firstPath, firstNode);
    try {
      migration.buildConfigurationFilePluginNodeMap();
      fail("Expected InvalidInputException");
    } catch (InvalidInputException e) {
      assertThat(e.getErrorCode(), is(ErrorCode.SAME_SERVICE_DEFINED_MULTIPLE_TIMES));
    }
  }

  @Test
  public void testValidateProvidedConfigurationWithMismatchedNumbers() {
    MigrationImpl migration = new MigrationImpl();

    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";
    String server4 = "server4";

    String stripe = "stripe";

    Map<Pair<String, String>, Node> hostConfigMapNode = new HashMap<>();

    hostConfigMapNode.put(new Pair<>(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server2), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server3), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server1);
    servers.add(server2);
    servers.add(server3);
    servers.add(server4);

    try {
      migration.validateProvidedConfiguration(hostConfigMapNode, servers);
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
              .map(pair -> pair.getAnother())
              .collect(Collectors.toSet())), is(true));
    }
  }

  @Test
  public void testValidateProvidedConfigurationWithDifferentServers() {
    MigrationImpl migration = new MigrationImpl();

    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";
    String server4 = "server4";

    String stripe = "stripe";

    Map<Pair<String, String>, Node> hostConfigMapNode = new HashMap<>();
    hostConfigMapNode.put(new Pair<>(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server2), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server3), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server1);
    servers.add(server2);
    servers.add(server4);
    try {
      migration.validateProvidedConfiguration(hostConfigMapNode, servers);
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
    MigrationImpl migration = new MigrationImpl();

    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";
    String server4 = "server4";

    String stripe = "stripe1";

    Map<Pair<String, String>, Node> hostConfigMapNode = new HashMap<>();
    hostConfigMapNode.put(new Pair<>(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server2), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server3), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server4), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server1);
    servers.add(server2);
    servers.add(server3);
    servers.add(server4);

    migration.validateProvidedConfiguration(hostConfigMapNode, servers);
  }

  @Test
  public void testMismatchedServers() {
    MigrationImpl migration = new MigrationImpl();

    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";

    String stripe = "stripe";

    Map<Pair<String, String>, Node> hostConfigMapNode = new HashMap<>();
    hostConfigMapNode.put(new Pair<>(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server2), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server3), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server1);
    servers.add(server2);
    servers.add(server3);

    Collection<String> mismatchedServers = migration.mismatchedServers(hostConfigMapNode, servers);
    assertThat(mismatchedServers.size(), is(0));
  }

  @Test
  public void testMismatchedServersNotAllMatching() {
    MigrationImpl migration = new MigrationImpl();

    String server1 = "server1";
    String server2 = "server2";
    String server3 = "server3";
    String server4 = "server4";

    String stripe = "stripe-1";

    Map<Pair<String, String>, Node> hostConfigMapNode = new HashMap<>();
    hostConfigMapNode.put(new Pair<>(stripe, server1), mock(Node.class));
    hostConfigMapNode.put(new Pair<>(stripe, server3), mock(Node.class));

    List<String> servers = new ArrayList<>();
    servers.add(server2);
    servers.add(server4);

    Collection<String> mismatchedServers = migration.mismatchedServers(hostConfigMapNode, servers);
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

    when(pluginsNode.getNamespaceURI()).thenReturn(MigrationImpl.TERRACOTTA_CONFIG_NAMESPACE);
    when(propertiesNode.getNamespaceURI()).thenReturn(MigrationImpl.TERRACOTTA_CONFIG_NAMESPACE);
    when(serversNode.getNamespaceURI()).thenReturn(MigrationImpl.TERRACOTTA_CONFIG_NAMESPACE);

    when(pluginsNode.getLocalName()).thenReturn(MigrationImpl.PLUGINS_NODE_NAME);
    when(propertiesNode.getLocalName()).thenReturn("tc-properties");
    when(serversNode.getLocalName()).thenReturn(MigrationImpl.SERVERS_NODE_NAME);

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

    when(serverNode1.getNamespaceURI()).thenReturn(MigrationImpl.TERRACOTTA_CONFIG_NAMESPACE);
    when(serverNode1.getLocalName()).thenReturn(MigrationImpl.SERVER_NODE_NAME);

    when(serverNode2.getNamespaceURI()).thenReturn(MigrationImpl.TERRACOTTA_CONFIG_NAMESPACE);
    when(serverNode2.getLocalName()).thenReturn(MigrationImpl.SERVER_NODE_NAME);

    when(reconnectionWindowNode.getNamespaceURI()).thenReturn(MigrationImpl.TERRACOTTA_CONFIG_NAMESPACE);
    when(reconnectionWindowNode.getLocalName()).thenReturn("client-reconnect-window");

    MigrationImpl migration = new MigrationImpl();
    MigrationImpl spyMigration = spy(migration);
    doReturn("server1").when(spyMigration).getAttributeValue(serverNode1, MigrationImpl.NAME_NODE_NAME);
    doReturn("server2").when(spyMigration).getAttributeValue(serverNode2, MigrationImpl.NAME_NODE_NAME);

    List<String> servers = spyMigration.extractServerNames(rootNode);

    assertThat(servers.size(), is(2));
    assertThat(servers.contains("server1"), is(true));
    assertThat(servers.contains("server2"), is(true));
  }
}