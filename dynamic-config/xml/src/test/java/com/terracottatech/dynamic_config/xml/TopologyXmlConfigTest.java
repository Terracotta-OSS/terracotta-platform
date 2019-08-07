/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Topology;
import com.terracottatech.utilities.Json;
import org.junit.Before;
import org.junit.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.terracottatech.utilities.Json.toPrettyJson;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

/**
 * @author Mathieu Carbou
 */
public class TopologyXmlConfigTest {

  private Topology topology;
  private String xml;

  @Before
  public void setUp() throws Exception {
    topology = Json.parse(getClass().getResource("/topology.json"), Topology.class);
    xml = new String(Files.readAllBytes(Paths.get(getClass().getResource("/multi-stripe-config.xml").toURI())), StandardCharsets.UTF_8);
  }

  @Test
  public void toXml() {
    String actual = TopologyXmlConfig.toXml(Paths.get(""), topology);
    assertThat(actual, actual, isSimilarTo(Input.from(xml))
        .ignoreComments()
        .ignoreWhitespace()
        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
  }

  @Test
  public void fromXml() {
    Topology actual = TopologyXmlConfig.fromXml("node-1", xml);
    assertThat(toPrettyJson(actual), actual, is(equalTo(topology)));
  }

}