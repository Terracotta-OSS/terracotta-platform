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
import org.terracotta.config.util.ParameterSubstitutor;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.json.Json;
import org.terracotta.testing.TmpDir;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.json.Json.toPrettyJson;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

/**
 * @author Mathieu Carbou
 */
public class OssConfigRepositoryMapperTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  private NodeContext nodeContext1, nodeContext2;
  private String xml1, xml2;
  private OssConfigRepositoryMapper xmlConfig;

  @Before
  public void setUp() throws URISyntaxException, IOException {
    PathResolver pathResolver = new PathResolver(
        Paths.get("%(user.dir)"),
        path -> path == null ? null : Paths.get(ParameterSubstitutor.substitute(path.toString())));
    xmlConfig = new OssConfigRepositoryMapper();
    xmlConfig.init(pathResolver);
    nodeContext1 = Json.parse(getClass().getResource("/topology1.json"), NodeContext.class);
    xml1 = new String(Files.readAllBytes(Paths.get(getClass().getResource("/topology1.xml").toURI())), StandardCharsets.UTF_8);
    nodeContext2 = Json.parse(getClass().getResource("/topology2.json"), NodeContext.class);
    xml2 = new String(Files.readAllBytes(Paths.get(getClass().getResource("/topology2.xml").toURI())), StandardCharsets.UTF_8);
  }

  @Test
  public void toXml1() {
    String actual = xmlConfig.toXml(nodeContext1).replace("\\", "/");
    assertThat(actual, actual, isSimilarTo(Input.from(xml1))
        .ignoreComments()
        .ignoreWhitespace()
        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
  }

  @Test
  public void fromXml1() {
    NodeContext actual = xmlConfig.fromXml("node-1", xml1);
    assertThat(toPrettyJson(actual), actual, is(equalTo(nodeContext1)));
  }

  @Test
  public void toXml2() {
    String actual = xmlConfig.toXml(nodeContext2).replace("\\", "/");
    assertThat(actual, actual, isSimilarTo(Input.from(xml2))
        .ignoreComments()
        .ignoreWhitespace()
        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
  }

  @Test
  public void fromXml2() {
    NodeContext actual = xmlConfig.fromXml("node-1", xml2);
    assertThat(toPrettyJson(actual), actual, is(equalTo(nodeContext2)));
  }

}