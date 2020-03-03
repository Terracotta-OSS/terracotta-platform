/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.xml.oss;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.testing.TmpDir;
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
import static org.terracotta.config.util.ParameterSubstitutor.substitute;
import static org.terracotta.dynamic_config.api.model.Node.newDefaultNode;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

public class OssXmlConfigurationTest {
  private final ClusterFactory clusterFactory = new ClusterFactory();

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  private PathResolver pathResolver;

  @Before
  public void setUp() {
    pathResolver = new PathResolver(
        Paths.get("%(user.dir)"),
        path -> path == null ? null : Paths.get(substitute(path.toString()))
    );
  }

  @Test
  public void testMarshallingRelativePath() throws Exception {
    Cluster cluster = new Cluster(new Stripe(newDefaultNode("node-1", "localhost")
        .setNodeLogDir(Paths.get("log"))
        .setNodeMetadataDir(Paths.get("metadata"))
        .setSecurityDir(Paths.get("security"))
        .setDataDir("main", Paths.get("bar"))
        .setNodeBackupDir(Paths.get("backup"))
        .setSecurityAuditLogDir(Paths.get("audit")))
    );
    String actual = new OssXmlConfiguration(cluster, 1, "node-1", pathResolver).toString();
    assertXml(actual, "node-1.xml");
  }

  @Test
  public void testMarshallingAbsolutePathWithPlaceHolders() throws Exception {
    Node node = newDefaultNode("node-2", "localhost");
    Cluster cluster = new Cluster(new Stripe(node));
    String actual = new OssXmlConfiguration(cluster, 1, "node-2", pathResolver).toString();
    assertXml(actual, "node-2.xml");
  }

  @Test
  public void testSingleStripe() throws Exception {
    String fileName = "single-stripe-config.properties";
    Cluster cluster = clusterFactory.create(loadProperties(fileName));

    String actual = new OssXmlConfiguration(cluster, 1, "node-1", pathResolver).toString();
    assertXml(actual, "single-stripe-config.xml");
  }

  @Test
  public void testMultiStripe() throws Exception {
    String fileName = "multi-stripe-config.properties";
    Cluster cluster = clusterFactory.create(loadProperties(fileName));

    String actual = new OssXmlConfiguration(cluster, 1, "node-1", pathResolver).toString();
    assertXml(actual, "multi-stripe-config.xml");
  }

  private void assertXml(String actual, String expectedConfigResource) throws URISyntaxException {
    actual = actual.replace("\\", "/");
    URI expectedConfigUrl = getClass().getResource("/" + expectedConfigResource).toURI();
    CompareMatcher matcher = isSimilarTo(Input.from(expectedConfigUrl))
        .ignoreComments()
        .ignoreWhitespace()
        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText));
    assertThat(actual, Input.from(actual), matcher);
  }

  private Properties loadProperties(String fileName) throws IOException, URISyntaxException {
    InputStream inputStream = Files.newInputStream(Paths.get(getClass().getResource("/" + fileName).toURI()));
    Properties properties = new Properties();
    properties.load(inputStream);
    return properties;
  }
}