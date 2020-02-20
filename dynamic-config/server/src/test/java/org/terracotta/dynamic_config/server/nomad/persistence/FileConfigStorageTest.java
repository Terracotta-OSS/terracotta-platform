/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.persistence;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.ConfigRepositoryMapper;
import org.terracotta.testing.TmpDir;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

public class FileConfigStorageTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  @Test
  public void saveAndRetrieve() throws Exception {
    Path root = temporaryFolder.getRoot();

    NodeContext topology = new NodeContext(new Cluster("bar", new Stripe(Node.newDefaultNode("node-1", "localhost"))), 1, "node-1");
    String xml = new String(Files.readAllBytes(Paths.get(getClass().getResource("/config.xml").toURI())), StandardCharsets.UTF_8).replace("\\", "/");

    FileConfigStorage storage = new FileConfigStorage(root, "node-1", new ConfigRepositoryMapper() {
      @Override
      public String toXml(NodeContext n) {
        assertThat(n, is(equalTo(topology)));
        return xml;
      }

      @Override
      public NodeContext fromXml(String nodeName, String x) {
        assertThat(nodeName, is(equalTo("node-1")));

        assertThat(x, x, isSimilarTo(Input.fromString(xml))
            .ignoreComments()
            .ignoreWhitespace()
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));

        return topology;
      }
    });

    assertFalse(Files.exists(root.resolve("node-1.1.xml")));
    storage.saveConfig(1L, topology);
    assertTrue(Files.exists(root.resolve("node-1.1.xml")));

    String xmlWritten = new String(Files.readAllBytes(root.resolve("node-1.1.xml")), StandardCharsets.UTF_8).replace("\\", "/");

    assertThat(xmlWritten, xmlWritten, isSimilarTo(Input.fromString(xml))
        .ignoreComments()
        .ignoreWhitespace()
        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));

    NodeContext loaded = storage.getConfig(1L);
    assertThat(loaded, is(topology));
  }
}