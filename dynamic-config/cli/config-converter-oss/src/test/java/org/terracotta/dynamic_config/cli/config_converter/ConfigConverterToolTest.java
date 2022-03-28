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
package org.terracotta.dynamic_config.cli.config_converter;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.testing.TmpDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
public class ConfigConverterToolTest {

  @Rule public TmpDir tmpDir = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);

  @Test
  public void test_conversion_1() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/tc-config-1.xml",
        "-n", "my-cluster",
        "-d", tmpDir.getRoot().resolve("generated-repositories").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-repositories").resolve("stripe-1").resolve("testServer0").resolve("config").resolve("testServer0.1.properties");
    assertTrue(Files.exists(config));
    assertThat(Props.load(getClass().getResourceAsStream("/cluster-1.properties")), is(equalTo(Props.load(config))));
  }
}
