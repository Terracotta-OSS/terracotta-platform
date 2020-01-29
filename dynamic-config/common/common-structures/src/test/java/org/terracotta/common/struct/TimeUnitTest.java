/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.common.struct;

import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TimeUnitTest {
  @Test
  public void testConversionFromMilliseconds() {
    assertThat(TimeUnit.MILLISECONDS.toMillis(1L), is(1L));
    assertThat(TimeUnit.MILLISECONDS.toSeconds(1000L), is(1L));
    assertThat(TimeUnit.MILLISECONDS.toMinutes(60000L), is(1L));
    assertThat(TimeUnit.MILLISECONDS.toHours(3600000L), is(1L));
  }

  @Test
  public void testConversionFromSeconds() {
    assertThat(TimeUnit.SECONDS.toMillis(1L), is(1000L));
    assertThat(TimeUnit.SECONDS.toSeconds(1L), is(1L));
    assertThat(TimeUnit.SECONDS.toMinutes(60L), is(1L));
    assertThat(TimeUnit.SECONDS.toHours(3600L), is(1L));
  }

  @Test
  public void testConversionFromMinutes() {
    assertThat(TimeUnit.MINUTES.toMillis(1L), is(60000L));
    assertThat(TimeUnit.MINUTES.toSeconds(1L), is(60L));
    assertThat(TimeUnit.MINUTES.toMinutes(1L), is(1L));
    assertThat(TimeUnit.MINUTES.toHours(60L), is(1L));
  }

  @Test
  public void testConversionFromHours() {
    assertThat(TimeUnit.HOURS.toMillis(1L), is(3600000L));
    assertThat(TimeUnit.HOURS.toSeconds(1L), is(3600L));
    assertThat(TimeUnit.HOURS.toMinutes(1L), is(60L));
    assertThat(TimeUnit.HOURS.toHours(1L), is(1L));
  }

  @Test(expected = ArithmeticException.class)
  public void testLongOverflow() {
    TimeUnit.HOURS.toMillis(10_000_000_000_000L);
  }

  @Test
  public void test_conversion() {
    Set<String> supportedUnits = Stream.of(TimeUnit.values()).map(TimeUnit::name).collect(Collectors.toSet());
    Stream.of(java.util.concurrent.TimeUnit.values()).forEach(jdkTimeUnit -> {
      if (supportedUnits.contains(jdkTimeUnit.name())) {
        assertThat(TimeUnit.from(jdkTimeUnit).isPresent(), is(true));
      } else {
        assertThat(TimeUnit.from(jdkTimeUnit).isPresent(), is(false));
      }
    });
  }

  @Test
  public void test_getBaseUnit() {
    Stream.of(TimeUnit.values()).forEach(unit -> assertThat(unit.getBaseUnit(), is(TimeUnit.SECONDS)));
  }

}