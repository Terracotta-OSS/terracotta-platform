/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.config.ConfigFileContainer;
import com.terracottatech.utilities.PathResolver;
import com.terracottatech.utilities.junit.TmpDir;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

public class XmlConfigurationTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  PathResolver pathResolver;

  @Before
  public void setUp() {
    pathResolver = new PathResolver(temporaryFolder.getRoot());
  }

  @Test
  public void testSingleStripe() throws Exception {
    String fileName = "single-stripe-config.properties";
    Cluster cluster = new ConfigFileContainer(fileName, loadProperties(fileName), "my-cluster").createCluster();

    String actual = new XmlConfiguration(cluster, 1, "node-1", pathResolver).toString();
    assertXml(actual, "single-stripe-config.xml");
  }

  @Test
  public void testMultiStripe() throws Exception {
    String fileName = "multi-stripe-config.properties";
    Cluster cluster = new ConfigFileContainer(fileName, loadProperties(fileName), "my-cluster").createCluster();

    String actual = new XmlConfiguration(cluster, 1, "node-1", pathResolver).toString();
    assertXml(actual, "multi-stripe-config.xml");
  }

  private void assertXml(String actual, String expectedConfigResource) throws URISyntaxException {
    actual = actual.replace(temporaryFolder.getRoot().toString() + "/", "")
        .replace(temporaryFolder.getRoot().toString() + "\\", "");
    URI expectedConfigUrl = getClass().getResource("/" + expectedConfigResource).toURI();
    CompareMatcher matcher = isSimilarTo(Input.from(expectedConfigUrl))
        .ignoreComments()
        .ignoreWhitespace()
        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText));
    assertThat(Input.from(actual), matcher);
  }

  private Properties loadProperties(String fileName) throws IOException, URISyntaxException {
    InputStream inputStream = Files.newInputStream(Paths.get(getClass().getResource("/" + fileName).toURI()));
    Properties properties = new Properties();
    properties.load(inputStream);
    return properties;
  }
}