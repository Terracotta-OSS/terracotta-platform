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
import org.terracotta.config.offheapresources.MemoryUnit;
import org.terracotta.config.offheapresources.OffheapResourcesType;
import org.terracotta.config.offheapresources.ResourceType;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.NonSubstitutingTCConfigurationParser;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.parsing.OffHeapResourceConfigurationParser;

import java.math.BigInteger;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * TcConfigWithOffheapTest
 */
public class TcConfigWithOffheapTest {

  @Test
  public void testCanParseConfigWithOffheap() throws Exception {
    TcConfig configuration = NonSubstitutingTCConfigurationParser.parse(getClass().getResourceAsStream("/configs/tc-config-offheap.xml"), getClass().getClassLoader());
    Config config = (Config) configuration.getPlugins().getConfigOrService().get(0);
    OffheapResourcesType resourcesType = new OffHeapResourceConfigurationParser().parse(config.getConfigContent());
    ResourceType resourceType = resourcesType.getResource().get(0);
    assertThat(resourceType.getName(), is(equalTo("main")));
    assertThat(resourceType.getUnit(), is(equalTo(MemoryUnit.MB)));
    assertThat(resourceType.getValue(), is(equalTo(BigInteger.valueOf(100))));
  }
}
