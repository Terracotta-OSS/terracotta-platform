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
package org.terracotta.persistence.sanskrit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class GroupingSpliteratorTest {
  private List<Deque<String>> groups;

  @Before
  public void before() {
    groups = new ArrayList<>();
  }

  @Test
  public void emptyStream() {
    Stream<String> lines = Stream.of();
    GroupingSpliterator spliterator = new GroupingSpliterator(lines);
    assertFalse(spliterator.tryAdvance(group -> groups.add(group)));

    assertTrue(groups.isEmpty());
  }

  @Test
  public void twoGroups() {
    Stream<String> lines = Stream.of("a", "b", "", "c", "");
    GroupingSpliterator spliterator = new GroupingSpliterator(lines);
    assertTrue(spliterator.tryAdvance(group -> groups.add(group)));
    assertTrue(spliterator.tryAdvance(group -> groups.add(group)));
    assertFalse(spliterator.tryAdvance(group -> groups.add(group)));

    assertEquals(2, groups.size());

    assertEquals(2, groups.get(0).size());
    assertEquals("a", groups.get(0).removeFirst());
    assertEquals("b", groups.get(0).removeFirst());

    assertEquals(1, groups.get(1).size());
    assertEquals("c", groups.get(1).removeFirst());
  }

  @Test
  public void trailingGroupIgnored() {
    Stream<String> lines = Stream.of("a", "b", "", "c");
    GroupingSpliterator spliterator = new GroupingSpliterator(lines);
    assertTrue(spliterator.tryAdvance(group -> groups.add(group)));
    assertFalse(spliterator.tryAdvance(group -> groups.add(group)));

    assertEquals(1, groups.size());

    assertEquals(2, groups.get(0).size());
    assertEquals("a", groups.get(0).removeFirst());
    assertEquals("b", groups.get(0).removeFirst());
  }
}
