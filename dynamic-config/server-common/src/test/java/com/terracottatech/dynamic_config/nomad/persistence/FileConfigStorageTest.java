/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.persistence;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.test.util.TmpDir;
import com.terracottatech.dynamic_config.util.ParameterSubstitutor;
import org.junit.Rule;
import org.junit.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

public class FileConfigStorageTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  private static final String NODE_NAME = "node-1";
  private Node node = Node.newDefaultNode(NODE_NAME, "localhost");
  private NodeContext topology = new NodeContext(new Cluster("bar", new Stripe(node)), 1, NODE_NAME);

  @Test
  public void saveAndRetrieve() throws Exception {
    Path root = temporaryFolder.getRoot();
    FileConfigStorage storage = new FileConfigStorage(root, NODE_NAME, new ParameterSubstitutor());

    storage.saveConfig(1L, topology);
    NodeContext loaded = storage.getConfig(1L);
    assertThat(loaded, is(topology));

    byte[] bytes = Files.readAllBytes(root.resolve("cluster-config.node-1.1.xml"));
    String actual = new String(bytes, StandardCharsets.UTF_8).replace("\\", "/");

    assertThat(actual, actual, isSimilarTo(Input.from(getClass().getResource("/config.xml")))
        .ignoreComments()
        .ignoreWhitespace()
        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
  }
}