/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.common;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class MultiConfigCommaSplitterTest {
  private static final MultiConfigCommaSplitter splitter = new MultiConfigCommaSplitter();

  //<--Get and unset command usages-->
  @Test
  public void testSplit_1() {
    List<String> split = splitter.split("offheap-resources.main");
    assertEquals(split, singletonList("offheap-resources.main"));
  }

  @Test
  public void testSplit_2() {
    List<String> split = splitter.split("offheap-resources");
    assertEquals(split, singletonList("offheap-resources"));
  }

  //<--Set command usages-->
  @Test
  public void testSplit_3() {
    List<String> split = splitter.split("offheap-resources.main=512MB");
    assertEquals(split, singletonList("offheap-resources.main=512MB"));
  }

  @Test
  public void testSplit_4() {
    List<String> split = splitter.split("offheap-resources=main:512MB");
    assertEquals(split, singletonList("offheap-resources.main=512MB"));
  }

  @Test
  public void testSplit_5() {
    List<String> split = splitter.split("offheap-resources=main:512MB,second:1GB");
    assertEquals(split, asList("offheap-resources.main=512MB", "offheap-resources.second=1GB"));
  }

  @Test
  public void testSplit_6() {
    List<String> split = splitter.split("stripe.1.node.1.tc-properties.something=value");
    assertEquals(split, singletonList("stripe.1.node.1.tc-properties.something=value"));
  }

  @Test
  public void testSplit_7() {
    List<String> split = splitter.split("client-reconnect-window=10s");
    assertEquals(split, singletonList("client-reconnect-window=10s"));
  }
}