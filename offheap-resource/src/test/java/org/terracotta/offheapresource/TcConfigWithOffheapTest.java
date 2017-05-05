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

package org.terracotta.offheapresource;

import org.junit.Test;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * TcConfigWithOffheapTest
 */
public class TcConfigWithOffheapTest {

  @Test
  public void testCanParseConfigWithOffheap() throws Exception {
    TcConfiguration configuration = TCConfigurationParser.parse(getClass().getResourceAsStream("/configs/tc-config-offheap.xml"));
    List<OffHeapResources> offHeapResources = configuration.getExtendedConfiguration(OffHeapResources.class);

    assertThat(offHeapResources, hasSize(1));
    Set<OffHeapResourceIdentifier> identifiers = offHeapResources.get(0).getAllIdentifiers();
    for (OffHeapResourceIdentifier identifier : identifiers) {
      assertThat(identifier.getName(), is("main"));
    }
  }
}
