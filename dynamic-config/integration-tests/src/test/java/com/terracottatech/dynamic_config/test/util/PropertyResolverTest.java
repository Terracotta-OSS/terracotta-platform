/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class PropertyResolverTest {

  @Test
  public void resolve() {
    Properties variables = new Properties();
    variables.setProperty("foo", "1");
    variables.setProperty("bar", "2");
    variables.setProperty("path", "c:\\a\\win\\path");
    PropertyResolver resolver = new PropertyResolver(variables);
    assertThat(resolver.resolve(null), is(nullValue()));
    assertThat(resolver.resolve("${a}"), is(equalTo("${a}")));
    assertThat(resolver.resolve("${foo}"), is(equalTo("1")));
    assertThat(resolver.resolve("${foo}${bar}"), is(equalTo("12")));
    assertThat(resolver.resolve("path/to/${foo}/and/${bar}"), is(equalTo("path/to/1/and/2")));
    assertThat(resolver.resolve("this is the ${path}"), is(equalTo("this is the c:\\a\\win\\path")));
  }

  @Test
  public void resolveAll() {
    Properties variables = new Properties();
    variables.setProperty("foo", "1");
    variables.setProperty("bar", "2");
    PropertyResolver resolver = new PropertyResolver(variables);

    Properties p = new Properties();
    p.setProperty("a", "");
    p.setProperty("b", "${a}");
    p.setProperty("c", "${foo}");
    p.setProperty("d", "${foo}${bar}");
    p.setProperty("e", "path/to/${foo}/and/${bar}");

    p = resolver.resolveAll(p);

    assertThat(p.entrySet(), hasSize(5));
    assertThat(p.getProperty("a"), is(equalTo("")));
    assertThat(p.getProperty("b"), is(equalTo("${a}")));
    assertThat(p.getProperty("c"), is(equalTo("1")));
    assertThat(p.getProperty("d"), is(equalTo("12")));
    assertThat(p.getProperty("e"), is(equalTo("path/to/1/and/2")));
  }
}