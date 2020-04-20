/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.cli.config_converter.validators;

import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ValidationException;
import org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCodeMapper;
import org.terracotta.dynamic_config.cli.config_converter.exception.InvalidInputConfigurationContentException;
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

  public void validate(ConfigValidator validator, Element element, Path path) {
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
