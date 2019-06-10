/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.validators;

import com.terracottatech.migration.exception.ErrorCodeMapper;
import com.terracottatech.migration.exception.InvalidInputConfigurationContentException;
import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ValidationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ValidationWrapper {

  private final ConfigValidator validator;

  public ValidationWrapper(ConfigValidator configValidator) {
    this.validator = configValidator;
  }

  public void check(Map<Path, Node> fileNodeMap) {
    AtomicReference<Node> previousNodes = new AtomicReference<>();
    AtomicReference<Path> previousPathRef = new AtomicReference<>();
    fileNodeMap.forEach((path, node) -> {
        validate(validator, (Element) node, path);
        if (previousNodes.get() != null) {
          compare(validator, (Element) node, (Element) previousNodes.get(), path, previousPathRef.get());
        }
      previousNodes.set(node);
      previousPathRef.set(path);
    });
  }

  void validate(ConfigValidator validator, Element element, Path path) {
    try {
      if (validator != null) {
        validator.validate(element);
      }
    } catch (ValidationException e) {
      ErrorCodeMapper.ErrorDetail errorDetail = ErrorCodeMapper.getErrorCode(e.getErrorId());
      throw new InvalidInputConfigurationContentException(errorDetail.getErrorCode(),
          String.format(errorDetail.getErrorMessage(), path.toString()));
    }
  }

  private void compare(ConfigValidator validator, Element elementOne, Element elementOther, Path pathOne, Path pathOther) {
    try {
      if (validator != null) {
        validator.validateAgainst(elementOne, elementOther);
      }
    } catch (ValidationException e) {
      ErrorCodeMapper.ErrorDetail errorDetail = ErrorCodeMapper.getErrorCode(e.getErrorId());
      throw new InvalidInputConfigurationContentException(errorDetail.getErrorCode(),
          String.format(errorDetail.getErrorMessage(), pathOne.toString(), pathOther.toString()));
    }
  }
}