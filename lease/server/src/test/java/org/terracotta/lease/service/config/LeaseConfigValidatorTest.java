/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.lease.service.config;

import org.junit.Test;
import org.terracotta.config.service.ValidationException;
import org.w3c.dom.Element;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@SuppressWarnings("unchecked")
public class LeaseConfigValidatorTest {

  @Test
  public void testValidate() throws ValidationException {
    Function<Element, LeaseElement> resourcesTypeSupplier = mock(Function.class);
    LeaseConfigValidator validator = new LeaseConfigValidator(resourcesTypeSupplier);
    LeaseConfigValidator spyValidator = spy(validator);

    Element xmlNode1 = mock(Element.class);
    Element xmlNode2 = mock(Element.class);

    LeaseElement leaseNode1 = new LeaseElement("100",TimeUnit.SECONDS.name());

    LeaseElement leaseNode2 = new LeaseElement("100", TimeUnit.SECONDS.name());

    doReturn(leaseNode1).when(spyValidator).createObject(xmlNode1);
    doReturn(leaseNode2).when(spyValidator).createObject(xmlNode2);

    spyValidator.validateAgainst(xmlNode1, xmlNode2);
  }

  @Test
  public void testValidateWithDifferentTimeUnits() throws ValidationException {
    Function<Element, LeaseElement> resourcesTypeSupplier = mock(Function.class);
    LeaseConfigValidator validator = new LeaseConfigValidator(resourcesTypeSupplier);
    LeaseConfigValidator spyValidator = spy(validator);

    Element xmlNode1 = mock(Element.class);
    Element xmlNode2 = mock(Element.class);

    LeaseElement leaseNode1 = new LeaseElement("120",TimeUnit.SECONDS.name());

    LeaseElement leaseNode2 = new LeaseElement("2", TimeUnit.MINUTES.name());

    doReturn(leaseNode1).when(spyValidator).createObject(xmlNode1);
    doReturn(leaseNode2).when(spyValidator).createObject(xmlNode2);

    spyValidator.validateAgainst(xmlNode1, xmlNode2);
  }

  @Test
  public void testValidateWithMismatchedLease() {
    Function<Element, LeaseElement> resourcesTypeSupplier = mock(Function.class);
    LeaseConfigValidator validator = new LeaseConfigValidator(resourcesTypeSupplier);
    LeaseConfigValidator spyValidator = spy(validator);

    Element xmlNode1 = mock(Element.class);
    Element xmlNode2 = mock(Element.class);


    LeaseElement leaseNode1 = new LeaseElement("200", TimeUnit.SECONDS.name());

    LeaseElement leaseNode2 = new LeaseElement("100", TimeUnit.SECONDS.name());

    doReturn(leaseNode1).when(spyValidator).createObject(xmlNode1);
    doReturn(leaseNode2).when(spyValidator).createObject(xmlNode2);

    try {
      spyValidator.validateAgainst(xmlNode1, xmlNode2);
      fail("Expected ValidationException");
    } catch (ValidationException e) {
      assertThat(e.getErrorId(), is(ValidationFailureId.LEASE_ENTRY_MISMATCH.getFailureId()));
    }
  }

  @Test
  public void testValidateWithMismatchedLeaseTimeUnit() {
    Function<Element, LeaseElement> resourcesTypeSupplier = mock(Function.class);
    LeaseConfigValidator validator = new LeaseConfigValidator(resourcesTypeSupplier);
    LeaseConfigValidator spyValidator = spy(validator);


    Element xmlNode1 = mock(Element.class);
    Element xmlNode2 = mock(Element.class);

    LeaseElement leaseNode1 = new LeaseElement("200", TimeUnit.SECONDS.name());

    LeaseElement leaseNode2 = new LeaseElement("200", TimeUnit.MILLISECONDS.name());

    doReturn(leaseNode1).when(spyValidator).createObject(xmlNode1);
    doReturn(leaseNode2).when(spyValidator).createObject(xmlNode2);


    try {
      spyValidator.validateAgainst(xmlNode1, xmlNode2);
      fail("Expected ValidationException");
    } catch (ValidationException e) {
      assertThat(e.getErrorId(), is(ValidationFailureId.LEASE_ENTRY_MISMATCH.getFailureId()));
    }
  }
}