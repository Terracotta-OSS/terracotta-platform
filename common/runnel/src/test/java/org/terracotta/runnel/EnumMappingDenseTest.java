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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.fail;

/**
 * Dense (array-backed) enum fast paths must behave exactly like the map-backed ones,
 * including across the dense/sparse boundary and for holes in the id range.
 */
public class EnumMappingDenseTest {

  enum Dense { A, B, C }
  enum Sparse { X, Y }
  enum Holes { P, Q }

  @Test
  public void denseMappingRoundTrips() {
    EnumMapping<Dense> m = EnumMappingBuilder.newEnumMappingBuilder(Dense.class)
        .mapping(Dense.A, 0).mapping(Dense.B, 1).mapping(Dense.C, 2).build();
    assertThat(m.toInt(Dense.A), is(0));
    assertThat(m.toInt(Dense.B), is(1));
    assertThat(m.toInt(Dense.C), is(2));
    assertThat(m.toEnum(0), is(Dense.A));
    assertThat(m.toEnum(1), is(Dense.B));
    assertThat(m.toEnum(2), is(Dense.C));
    assertThat(m.toEnum(3), nullValue());
    assertThat(m.toEnum(999999), nullValue());
    assertThat(m.toEnum(-3), nullValue());
  }

  @Test
  public void denseMappingWithHolesAndNonZeroBase() {
    EnumMapping<Holes> m = EnumMappingBuilder.newEnumMappingBuilder(Holes.class)
        .mapping(Holes.P, 10).mapping(Holes.Q, 30).build();
    assertThat(m.toInt(Holes.P), is(10));
    assertThat(m.toInt(Holes.Q), is(30));
    assertThat(m.toEnum(10), is(Holes.P));
    assertThat(m.toEnum(30), is(Holes.Q));
    assertThat(m.toEnum(20), nullValue()); // hole
    assertThat(m.toEnum(0), nullValue());
  }

  @Test
  public void sparseMappingFallsBackToMaps() {
    EnumMapping<Sparse> m = EnumMappingBuilder.newEnumMappingBuilder(Sparse.class)
        .mapping(Sparse.X, 1).mapping(Sparse.Y, 100_000).build();
    assertThat(m.toInt(Sparse.X), is(1));
    assertThat(m.toInt(Sparse.Y), is(100_000));
    assertThat(m.toEnum(1), is(Sparse.X));
    assertThat(m.toEnum(100_000), is(Sparse.Y));
    assertThat(m.toEnum(2), nullValue());
  }

  @Test
  public void sparseButSmallIdSpaceFallsBackToMaps() {
    // max id is within the hard cap, but a 4097-slot table for two entries would be all
    // holes: the density gate keeps this mapping map-backed and everything still works
    EnumMapping<Holes> m = EnumMappingBuilder.newEnumMappingBuilder(Holes.class)
        .mapping(Holes.P, 0).mapping(Holes.Q, 4096).build();
    assertThat(m.toInt(Holes.P), is(0));
    assertThat(m.toInt(Holes.Q), is(4096));
    assertThat(m.toEnum(0), is(Holes.P));
    assertThat(m.toEnum(4096), is(Holes.Q));
    assertThat(m.toEnum(2048), nullValue());
    assertThat(m.toEnum(4097), nullValue());
  }

  @Test
  public void duplicateAndNegativeValidationsPreserved() {
    try {
      EnumMappingBuilder.newEnumMappingBuilder(Dense.class).mapping(Dense.A, 0).mapping(Dense.A, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      EnumMappingBuilder.newEnumMappingBuilder(Dense.class).mapping(Dense.A, 0).mapping(Dense.B, 0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      EnumMappingBuilder.newEnumMappingBuilder(Dense.class).mapping(Dense.A, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      EnumMappingBuilder.newEnumMappingBuilder(Dense.class).mapping(Dense.A, 0).build(); // B, C missing
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void unmappedEnumThrows() {
    EnumMapping<Dense> m = EnumMappingBuilder.newEnumMappingBuilder(Dense.class)
        .mapping(Dense.A, 0).mapping(Dense.B, 1).mapping(Dense.C, 2).build();
    // toInt on a fully-registered JDK enum cannot fail; non-enum pseudo-enums can:
    EnumMapping<String> s = EnumMappingBuilder.newEnumMappingBuilder(String.class).mapping("a", 1).build();
    try {
      s.toInt("not mapped");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertThat(s.toEnum(1), is("a"));
    assertThat(s.toEnum(2), nullValue());
  }

  enum Wide { V, W, X, Y, Z }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void foreignEnumConstantIsRejectedAsUnmapped() {
    // The encoder API is unchecked (enm(String, E) infers E from the value), so a constant of
    // another enum type can reach toInt; the ordinal table must not map it by ordinal.
    EnumMapping dense = EnumMappingBuilder.newEnumMappingBuilder(Dense.class)
        .mapping(Dense.A, 10).mapping(Dense.B, 11).mapping(Dense.C, 12).build();
    EnumMapping sparse = EnumMappingBuilder.newEnumMappingBuilder(Dense.class)
        .mapping(Dense.A, 10).mapping(Dense.B, 11).mapping(Dense.C, 100_000).build();
    for (EnumMapping m : new EnumMapping[] {dense, sparse}) {
      assertThat(m.toInt(Dense.B), is(11));
      for (Enum<?> foreign : new Enum<?>[] {Wide.W /* ordinal in range */, Wide.Z /* out of range */}) {
        try {
          m.toInt(foreign);
          fail("foreign constant " + foreign + " was mapped");
        } catch (IllegalArgumentException expected) {
        }
      }
    }
  }

  @Test
  public void encoderRejectsForeignEnumConstant() {
    EnumMapping<Dense> m = EnumMappingBuilder.newEnumMappingBuilder(Dense.class)
        .mapping(Dense.A, 10).mapping(Dense.B, 11).mapping(Dense.C, 12).build();
    Struct struct = StructBuilder.newStructBuilder().enm("e", 1, m).build();
    try {
      struct.encoder().enm("e", Wide.W);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
