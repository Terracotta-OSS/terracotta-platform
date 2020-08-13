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
package org.terracotta.management.model.cluster;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.management.model.capabilities.descriptors.Settings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(JUnit4.class)
public class SettingsTest {
  @Test
  public void testCRUD() {
    Settings settings = new Settings();
    List<String> strings = Arrays.asList("foo", "bar", "baz");

    settings.set("strings", strings.toArray(new String[0]));
    String[] keyValue = settings.getStrings("strings");
    Set<String> keyValueSet = new HashSet<>(Arrays.asList(keyValue));
    assertTrue(keyValueSet.containsAll(strings));
  }

  @Test
  public void testSameStringsEquals() {
    Settings settings1 = new Settings();
    List<String> strings = Arrays.asList("foo", "bar", "baz");
    settings1.set("strings", strings.toArray(new String[0]));

    Settings settings2 = new Settings();
    settings2.set("strings", "foo", "bar", "baz");

    assertTrue(settings1.equals(settings2));
  }
}
