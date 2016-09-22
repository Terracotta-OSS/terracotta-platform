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
package org.terracotta.runnel;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ludovic Orban
 */
public class EnumMappingBuilderTest {

  private enum TestEnum {
    A,B,C
  }

  @Test
  public void testMappings() throws Exception {
    EnumMapping<TestEnum> enm = EnumMappingBuilder.newEnumMappingBuilder(TestEnum.class)
        .mapping(TestEnum.A, 10)
        .mapping(TestEnum.B, 20)
        .mapping(TestEnum.C, 30)
        .build();

    assertThat(enm.toInt(TestEnum.A), is(10));
    assertThat(enm.toInt(TestEnum.B), is(20));
    assertThat(enm.toInt(TestEnum.C), is(30));

    assertThat(enm.toEnum(10), CoreMatchers.<Enum>is(TestEnum.A));
    assertThat(enm.toEnum(20), CoreMatchers.<Enum>is(TestEnum.B));
    assertThat(enm.toEnum(30), CoreMatchers.<Enum>is(TestEnum.C));
  }

  @Test(expected = IllegalStateException.class)
  public void testUnmappedEnumInvalid() throws Exception {
    EnumMappingBuilder.newEnumMappingBuilder(TestEnum.class)
        .mapping(TestEnum.A, 10)
        .mapping(TestEnum.C, 30)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeIndexIsInvalid() throws Exception {
    EnumMappingBuilder.newEnumMappingBuilder(TestEnum.class)
        .mapping(TestEnum.A, -10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicateIndexIsInvalid() throws Exception {
    EnumMappingBuilder.newEnumMappingBuilder(TestEnum.class)
        .mapping(TestEnum.A, 10)
        .mapping(TestEnum.B, 10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicateEnumIsInvalid() throws Exception {
    EnumMappingBuilder.newEnumMappingBuilder(TestEnum.class)
        .mapping(TestEnum.A, 10)
        .mapping(TestEnum.A, 20);
  }

}
