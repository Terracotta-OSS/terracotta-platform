/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter;

import com.beust.jcommander.ParameterException;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.testing.TmpDir;

import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

public class ConfigConverterTest {
  @Rule
  public TmpDir tmpDir = new TmpDir(Paths.get(System.getProperty("user.dir"), "build"), false);

  @Test
  public void test_conversion_fail_cluster_name_missing() {
    ParameterException e = assertThrows(ParameterException.class, () -> new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/tc-config.xml",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f"));
    assertThat(e, hasMessage(equalTo("Cluster name is required for conversion into a configuration directory")));
  }

  @Test
  public void test_conversion_fail_already_existing_dir() {
    ParameterException e = assertThrows(ParameterException.class, () -> new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/tc-config.xml",
        "-n", "my-cluster",
        "-d", tmpDir.getRoot().toAbsolutePath().toString(),
        "-f"));
    assertThat(e, hasMessage(containsString("exists already. Please specify a non-existent directory")));
  }
}