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

import com.beust.jcommander.ParameterException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.testing.TmpDir;

import java.nio.file.Paths;

public class ConfigConverterTest {
  @Rule
  public TmpDir tmpDir = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void test_conversion_fail_cluster_name_missing() {
    exceptionRule.expect(ParameterException.class);
    exceptionRule.expectMessage("Cluster name is required for conversion into a configuration directory");
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/tc-config.xml",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
  }

  @Test
  public void test_conversion_fail_already_existing_dir() {
    exceptionRule.expect(ParameterException.class);
    exceptionRule.expectMessage("Please specify a non-existent directory");
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/tc-config.xml",
        "-n", "my-cluster",
        "-d", tmpDir.getRoot().toAbsolutePath().toString(),
        "-f");
  }
}