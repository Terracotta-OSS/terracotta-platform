/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model;

import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Properties;

import static java.lang.System.lineSeparator;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class PropsTest {

  private final Properties properties = new Properties();

  @Before
  public void setUp() {
    // not ordered
    properties.setProperty("c", "c");
    properties.setProperty("b", "b");
    properties.setProperty("a", "a");
    properties.setProperty("d", "d");
  }

  @Test
  public void test_store_without_comment() {
    StringWriter sw = new StringWriter();
    Props.store(sw, properties, null);
    assertThat(sw.toString(), is(equalTo(
        "a=a" + lineSeparator() +
            "b=b" + lineSeparator() +
            "c=c" + lineSeparator() +
            "d=d" + lineSeparator()
    )));
  }

  @Test
  public void test_store_wit_empty_comment() {
    StringWriter sw = new StringWriter();
    Props.store(sw, properties, "");
    assertThat(sw.toString(), is(equalTo(
        "#" + lineSeparator() +
            "a=a" + lineSeparator() +
            "b=b" + lineSeparator() +
            "c=c" + lineSeparator() +
            "d=d" + lineSeparator()
    )));
  }

  @Test
  public void test_store_wit_a_comment() {
    StringWriter sw = new StringWriter();
    Props.store(sw, properties, "My Comment");
    assertThat(sw.toString(), is(equalTo(
        "#My Comment" + lineSeparator() +
            "a=a" + lineSeparator() +
            "b=b" + lineSeparator() +
            "c=c" + lineSeparator() +
            "d=d" + lineSeparator()
    )));
  }

}