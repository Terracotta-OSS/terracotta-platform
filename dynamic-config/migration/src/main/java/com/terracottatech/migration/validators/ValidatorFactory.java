/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.validators;

import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.service.ConfigValidator;

import java.net.URI;
import java.util.function.Supplier;

public class ValidatorFactory {

  public static Supplier<ValidationWrapper> getParserValidatorValidator(URI uri) {
    ConfigValidator platformValidator = TCConfigurationParser.getValidator(uri);
    return () -> new ValidationWrapper(platformValidator);
  }
}