/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
