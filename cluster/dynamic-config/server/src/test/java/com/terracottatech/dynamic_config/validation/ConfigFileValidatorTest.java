/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.validation;

import com.terracottatech.dynamic_config.exception.MalformedConfigFileException;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;


public class ConfigFileValidatorTest {
  @Test
  public void testInsufficientKeys() {
    Properties properties = new Properties();
    properties.put("one.two.three", "something");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).startsWith("Invalid line: one.two.three");
    }
  }

  @Test
  public void testExtraKeys() {
    Properties properties = new Properties();
    properties.put("stripe.0.node.0.property.foo", "bar");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).startsWith("Invalid line: stripe.0.node.0.property.foo");
    }
  }

  @Test
  public void testUnknownNodeProperty() {
    Properties properties = new Properties();
    properties.put("stripe.0.node.0.blah", "something");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).startsWith("Unrecognized property: blah");
    }
  }

  @Test
  public void testMissingPropertyValue() {
    Properties properties = new Properties();
    properties.put("stripe.1.node.1.security-ssl-tls", "");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).contains("Missing value");
    }
  }
}