/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import org.junit.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.parsing.ConfigFileParser;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

public class XmlConfigurationTest {
  @Test
  public void testSingleStripe() throws Exception {
    Cluster cluster =
        ConfigFileParser.parse(new File(getClass().getResource("/single-stripe-config.properties").toURI()));

    String actual =
        new XmlConfiguration(cluster, "stripe-1", "node-1").toString();

    assertXml(actual, "single-stripe-config.xml");
  }

  @Test
  public void testMultiStripe() throws Exception {
    Cluster cluster =
        ConfigFileParser.parse(new File(getClass().getResource("/multi-stripe-config.properties").toURI()));

    String actual =
        new XmlConfiguration(cluster, "stripe-1", "node-1").toString();

    assertXml(actual, "multi-stripe-config.xml");
  }

  private void assertXml(String actual, String expectedConfigResource) throws URISyntaxException {
    URI expectedConfigUrl = getClass().getResource("/" + expectedConfigResource).toURI();
    assertThat(
        Input.from(actual),
        isSimilarTo(Input.from(expectedConfigUrl)).ignoreComments()
                                                  .ignoreWhitespace()
                                                  .withNodeMatcher(
                                                      new DefaultNodeMatcher(ElementSelectors.byNameAndText)
                                                  )
    );
  }
}