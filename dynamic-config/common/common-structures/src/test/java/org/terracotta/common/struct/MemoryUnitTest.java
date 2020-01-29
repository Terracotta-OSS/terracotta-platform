/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.common.struct;

import org.junit.Test;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class MemoryUnitTest {

  @Test
  public void convert() {
    assertThat(MemoryUnit.B.convert(4096, MemoryUnit.MB), is(equalTo(4096L * 1024 * 1024)));
    assertThat(MemoryUnit.KB.convert(4096, MemoryUnit.MB), is(equalTo(4096L * 1024)));
    assertThat(MemoryUnit.MB.convert(4096, MemoryUnit.MB), is(equalTo(4096L)));
    assertThat(MemoryUnit.GB.convert(4096, MemoryUnit.MB), is(equalTo(4L)));
    assertThat(MemoryUnit.TB.convert(4096, MemoryUnit.MB), is(equalTo(0L)));
  }

  @Test
  public void test_getBaseUnit() {
    Stream.of(MemoryUnit.values()).forEach(unit -> assertThat(unit.getBaseUnit(), is(MemoryUnit.B)));
  }
}