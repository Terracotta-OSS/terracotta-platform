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

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Ludovic Orban
 */
public class StructBuilderTest {

  private enum TestEnum {
    A, B, C
  }

  private static final Struct STRUCT = StructBuilder.newStructBuilder().build();
  private static final EnumMapping<TestEnum> ENM = EnumMappingBuilder.newEnumMappingBuilder(TestEnum.class)
      .mapping(TestEnum.A, 1)
      .mapping(TestEnum.B, 2)
      .mapping(TestEnum.C, 3)
      .build();

  @Test
  public void checkIndexZeroIsInvalid() throws Exception {
    try { StructBuilder.newStructBuilder().int32("a", 0); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int32s("a", 0); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64("a", 0); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64s("a", 0); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().string("a", 0); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().strings("a", 0); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().byteBuffer("a", 0); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64("a", 0); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64s("a", 0); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().struct("a", 0, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().structs("a", 0, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().enm("a", 0, ENM); fail(); } catch (IllegalArgumentException e) { /* expected */ }
  }

  @Test
  public void checkNegativeIndexIsInvalid() throws Exception {
    try { StructBuilder.newStructBuilder().int32("a", -1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int32s("a", -1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64("a", -1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64s("a", -1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().string("a", -1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().strings("a", -1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().byteBuffer("a", -1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64("a", -1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64s("a", -1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().struct("a", -1, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().structs("a", -1, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().enm("a", -1, ENM); fail(); } catch (IllegalArgumentException e) { /* expected */ }
  }

  @Test
  public void checkDuplicateIndexIsInvalid() throws Exception {
    try { StructBuilder.newStructBuilder().int32("a", 1).int32("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int32s("a", 1).int32s("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64("a", 1).int64("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64s("a", 1).int64s("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().string("a", 1).string("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().strings("a", 1).strings("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().byteBuffer("a", 1).byteBuffer("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64("a", 1).fp64("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64s("a", 1).fp64s("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().struct("a", 1, STRUCT).struct("b", 1, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().structs("a", 1, STRUCT).structs("b", 1, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().enm("a", 1, ENM).enm("b", 1, ENM); fail(); } catch (IllegalArgumentException e) { /* expected */ }
  }

  @Test
  public void checkShrinkingIndexIsInvalid() throws Exception {
    try { StructBuilder.newStructBuilder().int32("a", 2).int32("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int32s("a", 2).int32s("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64("a", 2).int64("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64s("a", 2).int64s("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().string("a", 2).string("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().strings("a", 2).strings("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().byteBuffer("a", 2).byteBuffer("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64("a", 2).fp64("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64s("a", 2).fp64s("b", 1); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().struct("a", 2, STRUCT).struct("b", 1, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().structs("a", 2, STRUCT).structs("b", 1, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().enm("a", 2, ENM).enm("b", 1, ENM); fail(); } catch (IllegalArgumentException e) { /* expected */ }
  }

  @Test
  public void checkDuplicateNameIsInvalid() throws Exception {
    try { StructBuilder.newStructBuilder().int32("a", 2).int32("a", 3); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int32s("a", 2).int32s("a", 3); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64("a", 2).int64("a", 3); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().int64s("a", 2).int64s("a", 3); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().string("a", 2).string("a", 3); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().strings("a", 2).strings("a", 3); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().byteBuffer("a", 2).byteBuffer("a", 3); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64("a", 2).fp64("a", 3); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().fp64s("a", 2).fp64s("a", 3); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().struct("a", 2, STRUCT).struct("a", 3, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().structs("a", 2, STRUCT).structs("a", 3, STRUCT); fail(); } catch (IllegalArgumentException e) { /* expected */ }
    try { StructBuilder.newStructBuilder().enm("a", 2, ENM).enm("a", 3, ENM); fail(); } catch (IllegalArgumentException e) { /* expected */ }
  }

  @Test
  public void checkValidConfigWorks() throws Exception {
    StructBuilder.newStructBuilder().int32("a", 2).int32("b", 3);
    StructBuilder.newStructBuilder().int32s("a", 2).int32s("b", 3);
    StructBuilder.newStructBuilder().int64("a", 2).int64("b", 3);
    StructBuilder.newStructBuilder().int64s("a", 2).int64s("b", 3);
    StructBuilder.newStructBuilder().string("a", 2).string("b", 3);
    StructBuilder.newStructBuilder().strings("a", 2).strings("b", 3);
    StructBuilder.newStructBuilder().byteBuffer("a", 2).byteBuffer("b", 3);
    StructBuilder.newStructBuilder().fp64("a", 2).fp64("b", 3);
    StructBuilder.newStructBuilder().fp64s("a", 2).fp64s("b", 3);
    StructBuilder.newStructBuilder().struct("a", 2, STRUCT).struct("b", 3, STRUCT);
    StructBuilder.newStructBuilder().structs("a", 2, STRUCT).structs("b", 3, STRUCT);
    StructBuilder.newStructBuilder().enm("a", 2, ENM).enm("b", 3, ENM);
  }

  @Test
  public void checkSimpleLazyStructAliasesWork() throws Exception {
    StructBuilder structBuilder = StructBuilder.newStructBuilder();

    Struct lazilyInitializedStruct = structBuilder.alias();
    Struct struct = structBuilder.int32("a", 2).int32("b", 3).build();

    lazilyInitializedStruct.encoder().int32("a", 1).int32("b", -1).encode();
    struct.encoder().int32("a", 1).int32("b", -1).encode();
  }

  @Test(expected = IllegalStateException.class)
  public void checkEncodingLazyStructAliasesThrowsWhenAliasedStructNotBuilt() throws Exception {
    StructBuilder abStructBuilder = StructBuilder.newStructBuilder().int32("a", 2).int32("b", 3);
    Struct lazilyInitializedAbStruct = abStructBuilder.alias();

    // build a struct using a not-yet-built struct alias
    Struct cdStruct = StructBuilder.newStructBuilder().int32("c", 5).int32("d", 6).struct("ab", 7, lazilyInitializedAbStruct).build();

    // using the struct should fail because the aliased struct hasn't been built yet
    cdStruct.encoder();
  }

  @Test(expected = IllegalStateException.class)
  public void checkDecodingLazyStructAliasesThrowsWhenAliasedStructNotBuilt() throws Exception {
    StructBuilder abStructBuilder = StructBuilder.newStructBuilder().int32("a", 2).int32("b", 3);
    Struct lazilyInitializedAbStruct = abStructBuilder.alias();

    // build a struct using a not-yet-built struct alias
    Struct cdStruct = StructBuilder.newStructBuilder().int32("c", 5).int32("d", 6).struct("ab", 7, lazilyInitializedAbStruct).build();

    // using the struct should fail because the aliased struct hasn't been built yet
    cdStruct.decoder(null);
  }

  @Test(expected = IllegalStateException.class)
  public void checkDumpingLazyStructAliasesThrowsWhenAliasedStructNotBuilt() throws Exception {
    StructBuilder abStructBuilder = StructBuilder.newStructBuilder().int32("a", 2).int32("b", 3);
    Struct lazilyInitializedAbStruct = abStructBuilder.alias();

    // build a struct using a not-yet-built struct alias
    Struct cdStruct = StructBuilder.newStructBuilder().int32("c", 5).int32("d", 6).struct("ab", 7, lazilyInitializedAbStruct).build();

    // using the struct should fail because the aliased struct hasn't been built yet
    cdStruct.dump(null, null);
  }

  @Test
  public void checkReferencedLazyStructAliasesWork() throws Exception {
    StructBuilder abStructBuilder = StructBuilder.newStructBuilder().int32("a", 2).int32("b", 3);
    Struct lazilyInitializedAbStruct = abStructBuilder.alias();

    // build a struct using a not-yet-built struct alias
    Struct cdStruct = StructBuilder.newStructBuilder().int32("c", 5).int32("d", 6).struct("ab", 7, lazilyInitializedAbStruct).build();

    // build the aliased struct
    abStructBuilder.build();

    // using the struct should work because the aliased struct has been built
    cdStruct.encoder().int32("c", 1).int32("d", -1).struct("ab").int32("a", 10).int32("b", -10).end().encode();
  }
}
