/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.model.exception.MalformedConfigFileException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Properties;

public class ConfigFileValidatorTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testInsufficientKeys() {
    Properties properties = new Properties();
    properties.put("one.two.three", "something");
    testThrowsWithMessage(properties, "Invalid line: one.two.three");
  }

  @Test
  public void testExtraKeys() {
    Properties properties = new Properties();
    properties.put("stripe.0.node.0.property.foo", "bar");
    testThrowsWithMessage(properties, "Invalid line: stripe.0.node.0.property.foo");
  }

  @Test
  public void testUnknownNodeProperty() {
    Properties properties = new Properties();
    properties.put("stripe.0.node.0.blah", "something");
    testThrowsWithMessage(properties, "Unrecognized property: blah");
  }

  @Test
  public void testMissingPropertyValue() {
    Properties properties = new Properties();
    properties.put("stripe.1.node.1.security-ssl-tls", "");
    testThrowsWithMessage(properties, "Missing value");
  }

  private void testThrowsWithMessage(Properties properties, String message) {
    exception.expect(MalformedConfigFileException.class);
    exception.expectMessage(message);
    ConfigFileValidator.validateProperties(properties, "test-file");
  }
}