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
package org.terracotta.config.data_roots;

import org.junit.Test;
import org.terracotta.config.service.ValidationException;
import org.terracotta.data.config.DataDirectories;
import org.terracotta.data.config.DataRootMapping;
import org.w3c.dom.Element;

import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataRootValidatorTest {

  @Test
  public void testValidate() throws ValidationException {
    @SuppressWarnings("unchecked")
    Function<Element, DataDirectories> resourcesTypeSupplier = mock(Function.class);
    DataRootValidator dataRootValidator = new DataRootValidator(resourcesTypeSupplier);

    DataRootMapping dataRootMapping1 = new DataRootMapping();
    dataRootMapping1.setName("dataRoot1");
    dataRootMapping1.setValue("../data-root1");
    dataRootMapping1.setUseForPlatform(true);

    DataRootMapping dataRootMapping2 = new DataRootMapping();
    dataRootMapping2.setName("dataRoot1");
    dataRootMapping2.setValue("../data-root2");
    dataRootMapping2.setUseForPlatform(false);

    DataDirectories dataDirectories = new DataDirectories();
    dataDirectories.getDirectory().add(dataRootMapping1);
    dataDirectories.getDirectory().add(dataRootMapping2);

    Element element = mock(Element.class);
    when(resourcesTypeSupplier.apply(element)).thenReturn(dataDirectories);
    dataRootValidator.validate(element);

    dataRootMapping2.setUseForPlatform(true);
    try {
      dataRootValidator.validate(element);
    } catch (ValidationException e) {
      assertEquals(ValidationFailureId.MULTIPLE_PLATFORM_DATA_ROOTS.getFailureId(), e.getErrorId());
    }
  }

  @Test
  public void testValidateAgainstUnequalNumberOfDataRoots() {
    @SuppressWarnings("unchecked")
    Function<Element, DataDirectories> resourcesTypeSupplier = mock(Function.class);
    DataRootValidator dataRootValidator = new DataRootValidator(resourcesTypeSupplier);

    DataDirectories dataDirectories1 = mock(DataDirectories.class);
    DataDirectories dataDirectories2 = mock(DataDirectories.class);

    @SuppressWarnings("unchecked")
    List<DataRootMapping> dataRootMappings1 = mock(List.class);
    @SuppressWarnings("unchecked")
    List<DataRootMapping> dataRootMappings2 = mock(List.class);

    when(dataDirectories1.getDirectory()).thenReturn(dataRootMappings1);
    when(dataDirectories2.getDirectory()).thenReturn(dataRootMappings2);

    when(dataRootMappings1.size()).thenReturn(2);
    when(dataRootMappings1.size()).thenReturn(3);

    Element element1 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element1)).thenReturn(dataDirectories1);

    Element element2 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element2)).thenReturn(dataDirectories2);

    try {
      dataRootValidator.validateAgainst(element1, element2);
    } catch (ValidationException e) {
      assertEquals(ValidationFailureId.MISMATCHED_DATA_DIR_RESOURCE_NUMBERS.getFailureId(), e.getErrorId());
    }
  }

  @Test
  public void testValidateAgainstWithNameMismatch() {
    @SuppressWarnings("unchecked")
    Function<Element, DataDirectories> resourcesTypeSupplier = mock(Function.class);
    DataRootValidator dataRootValidator = new DataRootValidator(resourcesTypeSupplier);

    DataRootMapping dataRootMapping1 = new DataRootMapping();
    dataRootMapping1.setName("dataRoot1");
    dataRootMapping1.setValue("../data-root1");
    dataRootMapping1.setUseForPlatform(true);

    DataRootMapping dataRootMapping2 = new DataRootMapping();
    dataRootMapping2.setName("dataRoot2");
    dataRootMapping2.setValue("../data-root2");
    dataRootMapping1.setUseForPlatform(true);

    DataDirectories dataDirectories1 = new DataDirectories();
    DataDirectories dataDirectories2 = new DataDirectories();

    dataDirectories1.getDirectory().add(dataRootMapping1);
    dataDirectories2.getDirectory().add(dataRootMapping2);

    Element element1 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element1)).thenReturn(dataDirectories1);

    Element element2 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element2)).thenReturn(dataDirectories2);

    try {
      dataRootValidator.validateAgainst(element1, element2);
    } catch (ValidationException e) {
      assertEquals(ValidationFailureId.MISMATCHED_DATA_DIR_NAMES.getFailureId(), e.getErrorId());
    }
  }

  @Test
  public void testValidateAgainstWithDifferentPlatforms() {
    @SuppressWarnings("unchecked")
    Function<Element, DataDirectories> resourcesTypeSupplier = mock(Function.class);
    DataRootValidator dataRootValidator = new DataRootValidator(resourcesTypeSupplier);

    DataRootMapping dataRootMapping11 = new DataRootMapping();
    dataRootMapping11.setName("dataRoot1");
    dataRootMapping11.setValue("../data-root1");
    dataRootMapping11.setUseForPlatform(true);
    DataRootMapping dataRootMapping12 = new DataRootMapping();
    dataRootMapping12.setName("dataRoot2");
    dataRootMapping12.setValue("../data-root2");
    dataRootMapping12.setUseForPlatform(false);

    DataRootMapping dataRootMapping21 = new DataRootMapping();
    dataRootMapping21.setName("dataRoot1");
    dataRootMapping21.setValue("../data-root1");
    dataRootMapping21.setUseForPlatform(false);
    DataRootMapping dataRootMapping22 = new DataRootMapping();
    dataRootMapping22.setName("dataRoot2");
    dataRootMapping22.setValue("../data-root2");
    dataRootMapping22.setUseForPlatform(true);

    DataDirectories dataDirectories1 = new DataDirectories();
    DataDirectories dataDirectories2 = new DataDirectories();

    dataDirectories1.getDirectory().add(dataRootMapping11);
    dataDirectories1.getDirectory().add(dataRootMapping12);
    dataDirectories2.getDirectory().add(dataRootMapping21);
    dataDirectories2.getDirectory().add(dataRootMapping22);

    Element element1 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element1)).thenReturn(dataDirectories1);

    Element element2 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element2)).thenReturn(dataDirectories2);

    try {
      dataRootValidator.validateAgainst(element1, element2);
    } catch (ValidationException e) {
      assertEquals(ValidationFailureId.DIFFERENT_PLATFORM_DATA_ROOTS.getFailureId(), e.getErrorId());
    }
  }

  @Test
  public void testValidateAgainstWithPlatformMissing() {
    @SuppressWarnings("unchecked")
    Function<Element, DataDirectories> resourcesTypeSupplier = mock(Function.class);
    DataRootValidator dataRootValidator = new DataRootValidator(resourcesTypeSupplier);

    DataRootMapping dataRootMapping11 = new DataRootMapping();
    dataRootMapping11.setName("dataRoot1");
    dataRootMapping11.setValue("../data-root1");
    dataRootMapping11.setUseForPlatform(true);
    DataRootMapping dataRootMapping12 = new DataRootMapping();
    dataRootMapping12.setName("dataRoot2");
    dataRootMapping12.setValue("../data-root2");
    dataRootMapping12.setUseForPlatform(false);

    DataRootMapping dataRootMapping21 = new DataRootMapping();
    dataRootMapping21.setName("dataRoot1");
    dataRootMapping21.setValue("../data-root1");
    dataRootMapping21.setUseForPlatform(false);
    DataRootMapping dataRootMapping22 = new DataRootMapping();
    dataRootMapping22.setName("dataRoot2");
    dataRootMapping22.setValue("../data-root2");
    dataRootMapping22.setUseForPlatform(false);

    DataDirectories dataDirectories1 = new DataDirectories();
    DataDirectories dataDirectories2 = new DataDirectories();

    dataDirectories1.getDirectory().add(dataRootMapping11);
    dataDirectories1.getDirectory().add(dataRootMapping12);
    dataDirectories2.getDirectory().add(dataRootMapping21);
    dataDirectories2.getDirectory().add(dataRootMapping22);

    Element element1 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element1)).thenReturn(dataDirectories1);

    Element element2 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element2)).thenReturn(dataDirectories2);

    try {
      dataRootValidator.validateAgainst(element1, element2);
    } catch (ValidationException e) {
      assertEquals(ValidationFailureId.PLATFORM_DATA_ROOT_MISSING_IN_ONE.getFailureId(), e.getErrorId());
    }
  }

  @Test
  public void testValidateAgainst() throws ValidationException {
    @SuppressWarnings("unchecked")
    Function<Element, DataDirectories> resourcesTypeSupplier = mock(Function.class);
    DataRootValidator dataRootValidator = new DataRootValidator(resourcesTypeSupplier);

    DataRootMapping dataRootMapping11 = new DataRootMapping();
    dataRootMapping11.setName("dataRoot1");
    dataRootMapping11.setValue("../data-root1");
    dataRootMapping11.setUseForPlatform(true);
    DataRootMapping dataRootMapping12 = new DataRootMapping();
    dataRootMapping12.setName("dataRoot2");
    dataRootMapping12.setValue("../data-root2");
    dataRootMapping12.setUseForPlatform(false);

    DataRootMapping dataRootMapping21 = new DataRootMapping();
    dataRootMapping21.setName("dataRoot1");
    dataRootMapping21.setValue("../data-root1");
    dataRootMapping21.setUseForPlatform(true);
    DataRootMapping dataRootMapping22 = new DataRootMapping();
    dataRootMapping22.setName("dataRoot2");
    dataRootMapping22.setValue("../data-root2");
    dataRootMapping22.setUseForPlatform(false);

    DataDirectories dataDirectories1 = new DataDirectories();
    DataDirectories dataDirectories2 = new DataDirectories();

    dataDirectories1.getDirectory().add(dataRootMapping11);
    dataDirectories1.getDirectory().add(dataRootMapping12);
    dataDirectories2.getDirectory().add(dataRootMapping21);
    dataDirectories2.getDirectory().add(dataRootMapping22);

    Element element1 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element1)).thenReturn(dataDirectories1);

    Element element2 = mock(Element.class);
    when(resourcesTypeSupplier.apply(element2)).thenReturn(dataDirectories2);

    dataRootValidator.validateAgainst(element1, element2);
  }
}
