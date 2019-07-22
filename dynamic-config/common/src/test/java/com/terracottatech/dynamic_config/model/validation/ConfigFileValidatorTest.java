/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.model.exception.MalformedConfigFileException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Properties;

public class ConfigFileValidatorTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private ConfigFileValidator configFileValidator;
  private Properties properties;

  @Before
  public void setUp() {
    properties = new Properties();
    configFileValidator = new ConfigFileValidator("test-file", properties);
  }

  @Test
  public void testInsufficientKeys() {
    properties.put("one.two.three", "something");
    testThrowsWithMessage("Invalid line: one.two.three");
  }

  @Test
  public void testExtraKeys() {
    properties.put("stripe.0.node.0.property.foo", "bar");
    testThrowsWithMessage("Invalid line: stripe.0.node.0.property.foo");
  }

  @Test
  public void testUnknownNodeProperty() {
    properties.put("stripe.0.node.0.blah", "something");
    testThrowsWithMessage("Unrecognized property: blah");
  }

  @Test
  public void testMissingPropertyValue() {
    properties.put("stripe.1.node.1.security-ssl-tls", "");
    testThrowsWithMessage("Missing value");
  }

  private void testThrowsWithMessage(String message) {
    exception.expect(MalformedConfigFileException.class);
    exception.expectMessage(message);
    configFileValidator.validate();
  }
}