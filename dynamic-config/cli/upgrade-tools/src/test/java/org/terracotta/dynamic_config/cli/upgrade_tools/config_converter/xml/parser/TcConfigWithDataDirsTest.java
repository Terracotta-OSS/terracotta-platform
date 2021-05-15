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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.parser;

import org.junit.Test;
import org.terracotta.config.Config;
import org.terracotta.config.TcConfig;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.NonSubstitutingTCConfigurationParser;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.parsing.DataRootConfigParser;
import org.terracotta.config.dataroots.DataDirectories;
import org.terracotta.config.dataroots.DataRootMapping;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * TcConfigWithDataDirectoriesTest
 */
public class TcConfigWithDataDirsTest {

  @Test
  public void testCanParseConfigWithDataRoot() throws Exception {
    TcConfig configuration = NonSubstitutingTCConfigurationParser.parse(getClass().getResourceAsStream("/configs/tc-config-data.xml"), getClass().getClassLoader());
    Config config = (Config) configuration.getPlugins().getConfigOrService().get(0);
    DataDirectories dataDirectories = new DataRootConfigParser().parse(config.getConfigContent());
    List<DataRootMapping> dataRoots = dataDirectories.getDirectory();

    assertThat(dataRoots, hasSize(2));

    assertThat(dataRoots.get(0).getName(), is(equalTo("data")));
    assertThat(dataRoots.get(0).getValue(), is(equalTo("data")));
    assertTrue(dataRoots.get(0).isUseForPlatform());

    assertThat(dataRoots.get(1).getName(), is(equalTo("other")));
    assertThat(dataRoots.get(1).getValue(), is(equalTo("other")));
    assertFalse(dataRoots.get(1).isUseForPlatform());
  }
}
