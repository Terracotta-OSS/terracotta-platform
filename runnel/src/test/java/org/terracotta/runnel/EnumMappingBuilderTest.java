/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Ludovic Orban
 */
@SuppressWarnings("rawtypes")
public class EnumMappingBuilderTest {

  private enum TestEnum {
    A,B,C
  }

  private static final Character X = 'x';
  private static final Character Y = 'y';
  private static final Character Z = 'z';

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicateEnumIsInvalidWithNonEnum() throws Exception {
    EnumMappingBuilder.newEnumMappingBuilder(Character.class)
        .mapping(X, 1)
        .mapping(X, 2)
        ;
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicateIndexIsInvalidWithNonEnum() throws Exception {
    EnumMappingBuilder.newEnumMappingBuilder(Character.class)
        .mapping(X, 1)
        .mapping(Y, 1)
        ;
  }

  @Test
  public void testNonEnumTypeEnum() throws Exception {
    EnumMapping<Character> enm = EnumMappingBuilder.newEnumMappingBuilder(Character.class)
        .mapping(X, 1)
        .mapping(Y, 2)
        .mapping(Z, 3)
        .build();

    assertThat(enm.toInt(X), is(1));
    assertThat(enm.toInt(Y), is(2));
    assertThat(enm.toInt(Z), is(3));
    try { enm.toInt('1'); fail(); } catch (IllegalArgumentException e) { /* expected */ }

    assertThat(enm.toEnum(1), is(X));
    assertThat(enm.toEnum(2), is(Y));
    assertThat(enm.toEnum(3), is(Z));
    assertThat(enm.toEnum(100), is(nullValue()));
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
