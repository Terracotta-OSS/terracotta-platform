/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.migration.validators;

import com.terracottatech.dynamic_config.server.migration.exception.InvalidInputConfigurationContentException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ValidationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ValidationWrapperTest {

  @Test
  public void testCheck() throws ValidationException {
    ConfigValidator validator = mock(ConfigValidator.class);
    ValidationWrapper validationWrapper = new ValidationWrapper(validator);

    ValidationWrapper spyValidationWrapper = spy(validationWrapper);
    Map<Path, Node> param = new HashMap<>();


    Path path1 = mock(Path.class);
    Element element1 = mock(Element.class);
    param.put(path1, element1);

    Path path2 = mock(Path.class);
    Element element2 = mock(Element.class);
    param.put(path2, element2);

    Path path3 = mock(Path.class);
    Element element3 = mock(Element.class);
    param.put(path3, element3);

    doNothing().when(validator).validate(element1);
    doNothing().when(validator).validate(element2);
    doNothing().when(validator).validate(element3);
    doNothing().when(validator).validateAgainst(element2, element1);
    doNothing().when(validator).validateAgainst(element3, element2);

    ArgumentCaptor<Element> elementArgumentCaptor1 = ArgumentCaptor.forClass(Element.class);
    ArgumentCaptor<Element> elementArgumentCaptor2 = ArgumentCaptor.forClass(Element.class);
    ArgumentCaptor<Element> elementArgumentCaptor3 = ArgumentCaptor.forClass(Element.class);

    spyValidationWrapper.check(param);

    verify(validator, times(3)).validate(elementArgumentCaptor1.capture());
    verify(validator, times(2)).validateAgainst(elementArgumentCaptor2.capture(), elementArgumentCaptor3.capture());

    assertThat(elementArgumentCaptor1.getAllValues()
        .containsAll(Arrays.asList(element1, element2, element3)), is(true));

    List<Element> invokedElementParamList = new ArrayList<>();
    invokedElementParamList.addAll(elementArgumentCaptor2.getAllValues());
    invokedElementParamList.addAll(elementArgumentCaptor3.getAllValues());

    assertThat(invokedElementParamList.containsAll(Arrays.asList(element3, element2, element1)), is(true));
  }

  @Test
  public void testCheckThrowingExceptionInValidate() throws ValidationException {

    ConfigValidator validator = mock(ConfigValidator.class);

    ValidationWrapper validationWrapper = new ValidationWrapper(validator);

    ValidationWrapper spyValidationWrapper = spy(validationWrapper);
    Map<Path, Node> param = new HashMap<>();

    Path path1 = mock(Path.class);
    Element element1 = mock(Element.class);
    param.put(path1, element1);

    Path path2 = mock(Path.class);
    Element element2 = mock(Element.class);
    param.put(path2, element2);

    Path path3 = mock(Path.class);
    Element element3 = mock(Element.class);
    param.put(path3, element3);

    ValidationException validationException = mock(ValidationException.class);

    doNothing().when(validator).validate(element1);
    doNothing().when(validator).validate(element2);
    doThrow(validationException).when(validator).validate(element3);
    doNothing().when(validator).validateAgainst(element2, element1);
    doNothing().when(validator).validateAgainst(element3, element2);

    try {
      spyValidationWrapper.check(param);
      fail("Expected InvalidInputConfigurationContentException");
    } catch (InvalidInputConfigurationContentException e) {
      //Do nothing
    }

  }

  @Test
  public void testCheckThrowingExceptionInValidateAgainst() throws ValidationException {
    ConfigValidator validator = mock(ConfigValidator.class);

    ValidationWrapper validationWrapper = new ValidationWrapper(validator);

    ValidationWrapper spyValidationWrapper = spy(validationWrapper);
    Map<Path, Node> param = new HashMap<>();

    Path path1 = mock(Path.class);
    Element element1 = mock(Element.class);
    param.put(path1, element1);

    Path path2 = mock(Path.class);
    Element element2 = mock(Element.class);
    param.put(path2, element2);

    Path path3 = mock(Path.class);
    Element element3 = mock(Element.class);
    param.put(path3, element3);

    ValidationException validationException = mock(ValidationException.class);

    doNothing().when(validator).validate(element1);
    doNothing().when(validator).validate(element2);
    doNothing().when(validator).validate(element3);
    doThrow(validationException).when(validator).validateAgainst(element2, element1);
    doThrow(validationException).when(validator).validateAgainst(element1, element2);
    doThrow(validationException).when(validator).validateAgainst(element3, element1);
    doThrow(validationException).when(validator).validateAgainst(element1, element3);
    doThrow(validationException).when(validator).validateAgainst(element3, element2);
    doThrow(validationException).when(validator).validateAgainst(element2, element3);

    try {
      spyValidationWrapper.check(param);
      fail("Expected InvalidInputConfigurationContentException");
    } catch (InvalidInputConfigurationContentException e) {

    }

  }

  @Test(expected = InvalidInputConfigurationContentException.class)
  public void testValidate() throws ValidationException {
    ConfigValidator validator = mock(ConfigValidator.class);
    ValidationWrapper validationWrapper = new ValidationWrapper(validator);
    Path path = mock(Path.class);

    Element element = mock(Element.class);
    ValidationException thrownException = mock(ValidationException.class);
    doThrow(thrownException).when(validator).validate(element);
    validationWrapper.validate(validator, element, path);
  }

  @Test
  public void testValidateWithNoValidator() {
    ConfigValidator configValidator = null;
    ValidationWrapper validationWrapper = new ValidationWrapper(configValidator);
    Path path = mock(Path.class);

    Element element = mock(Element.class);
    validationWrapper.validate(configValidator, element, path);
  }
}