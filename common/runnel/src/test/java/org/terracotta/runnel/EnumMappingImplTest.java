/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
import org.terracotta.runnel.decoding.StructDecoder;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.fail;

/**
 * Coverage of the four {@link EnumMapping} implementations and of the builder's selection
 * between them: every combination of (Java enum vs pseudo-enum) x (dense vs sparse id space)
 * must honor the same {@link EnumMapping} contract - round-tripping, {@code null} for
 * unmapped ids, and {@link IllegalArgumentException} for unmapped constants (including null
 * and constants of unrelated enum types, whose ordinals may fall inside the lookup tables).
 */
public class EnumMappingImplTest {

  enum NoConstants { }
  enum TrafficLight { RED, YELLOW, GREEN }
  enum Scattered { P, Q }
  enum Wide { X, Y }
  enum Foreign { F0, F1 }

  @SuppressWarnings("unchecked")
  private static <E> EnumMapping<E> mapping(Class<E> type, Object... pairs) {
    EnumMappingBuilder<E> builder = EnumMappingBuilder.newEnumMappingBuilder(type);
    for (int i = 0; i < pairs.length; i += 2) {
      builder.mapping((E) pairs[i], (Integer) pairs[i + 1]);
    }
    return builder.build();
  }

  private static String impl(EnumMapping<?> mapping) {
    return mapping.getClass().getSimpleName();
  }

  /**
   * Asserts the {@link EnumMapping} contract: each key round-trips through its id, each id
   * round-trips back to the same instance, unmapped ids answer {@code null}, and unmapped
   * constants - including null and a constant of an unrelated enum type - answer
   * {@link IllegalArgumentException}.
   */
  @SuppressWarnings("unchecked")
  private static <E> void assertContract(EnumMapping<E> mapping, E[] keys, int[] ids, int[] unmappedIds, Object foreign) {
    for (int i = 0; i < keys.length; i++) {
      assertThat(mapping.toInt(keys[i]), is(ids[i]));
      assertThat("toEnum(" + ids[i] + ")", mapping.toEnum(ids[i]), is(keys[i]));
    }
    for (int unmappedId : unmappedIds) {
      assertThat("toEnum(" + unmappedId + ")", mapping.toEnum(unmappedId), nullValue());
    }
    for (Object unmapped : new Object[] {null, foreign}) {
      try {
        mapping.toInt((E) unmapped);
        fail("expected IllegalArgumentException for " + unmapped);
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void builderSelectsImplementation() {
    // Java enum, dense ids -> arrays in both directions
    assertThat(impl(mapping(TrafficLight.class, TrafficLight.RED, 0, TrafficLight.YELLOW, 1, TrafficLight.GREEN, 2)), is("DenseOrdinalEnumMapping"));
    // Java enum, small id space with holes -> still dense (footprint floor)
    assertThat(impl(mapping(Scattered.class, Scattered.P, 10, Scattered.Q, 30)), is("DenseOrdinalEnumMapping"));
    // Java enum, sparse ids -> ordinal table + int->enum map
    assertThat(impl(mapping(Wide.class, Wide.X, 1, Wide.Y, 100_000)), is("SparseOrdinalEnumMapping"));
    // Java enum, no constants -> sparse (an empty id space is never dense)
    assertThat(impl(mapping(NoConstants.class)), is("SparseOrdinalEnumMapping"));
    // pseudo-enum, dense ids
    assertThat(impl(mapping(String.class, "a", 0, "b", 1)), is("DenseEnumMapping"));
    // pseudo-enum, sparse ids
    assertThat(impl(mapping(String.class, "a", 1, "b", 100_000)), is("MapEnumMapping"));
    // pseudo-enum, empty mapping
    assertThat(impl(mapping(String.class)), is("MapEnumMapping"));
  }

  @Test
  public void denseOrdinalMappingHonorsContract() {
    EnumMapping<TrafficLight> mapping = mapping(TrafficLight.class,
        TrafficLight.RED, 0, TrafficLight.YELLOW, 1, TrafficLight.GREEN, 2);
    assertThat(impl(mapping), is("DenseOrdinalEnumMapping"));
    assertContract(mapping,
        new TrafficLight[] {TrafficLight.RED, TrafficLight.YELLOW, TrafficLight.GREEN},
        new int[] {0, 1, 2},
        new int[] {3, -1, 999_999},
        Foreign.F0);

    // dense with holes: unmapped ids inside the table's range answer null
    EnumMapping<Scattered> holed = mapping(Scattered.class, Scattered.P, 10, Scattered.Q, 30);
    assertThat(impl(holed), is("DenseOrdinalEnumMapping"));
    assertContract(holed,
        new Scattered[] {Scattered.P, Scattered.Q},
        new int[] {10, 30},
        new int[] {0, 11, 20, 29, 31, -5},
        Foreign.F1);
  }

  @Test
  public void sparseOrdinalMappingHonorsContract() {
    EnumMapping<Wide> mapping = mapping(Wide.class, Wide.X, 1, Wide.Y, 100_000);
    assertThat(impl(mapping), is("SparseOrdinalEnumMapping"));
    assertContract(mapping,
        new Wide[] {Wide.X, Wide.Y},
        new int[] {1, 100_000},
        new int[] {0, 2, 100_001, -1},
        Foreign.F0);
  }

  @Test
  public void densePseudoEnumMappingHonorsContract() {
    EnumMapping<String> mapping = mapping(String.class, "a", 0, "b", 1);
    assertThat(impl(mapping), is("DenseEnumMapping"));
    assertContract(mapping,
        new String[] {"a", "b"},
        new int[] {0, 1},
        new int[] {2, -1, 999_999},
        "not mapped");
  }

  @Test
  public void mapPseudoEnumMappingHonorsContract() {
    EnumMapping<String> mapping = mapping(String.class, "a", 1, "b", 100_000);
    assertThat(impl(mapping), is("MapEnumMapping"));
    assertContract(mapping,
        new String[] {"a", "b"},
        new int[] {1, 100_000},
        new int[] {0, 2, 100_001},
        "not mapped");
  }

  @Test
  public void emptyMappingsHonorContract() {
    EnumMapping<NoConstants> javaEnum = mapping(NoConstants.class);
    assertThat(impl(javaEnum), is("SparseOrdinalEnumMapping"));
    assertThat(javaEnum.toEnum(0), nullValue());
    assertThat(javaEnum.toEnum(-1), nullValue());
    assertContract(javaEnum, new NoConstants[0], new int[0], new int[] {0, 1, -1}, Foreign.F0);

    EnumMapping<String> pseudoEnum = mapping(String.class);
    assertThat(impl(pseudoEnum), is("MapEnumMapping"));
    assertThat(pseudoEnum.toEnum(0), nullValue());
    assertContract(pseudoEnum, new String[0], new int[0], new int[] {0, 1, -1}, "a");
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void foreignEnumWithInRangeOrdinalIsRejectedByIdentityGuard() {
    // Foreign.F0 has ordinal 0, which is inside TrafficLight's ordinal table: the identity
    // guard must reject it rather than answering TrafficLight's id for the foreign constant
    EnumMapping<TrafficLight> mapping = mapping(TrafficLight.class,
        TrafficLight.RED, 0, TrafficLight.YELLOW, 1, TrafficLight.GREEN, 2);
    EnumMapping raw = mapping;
    try {
      raw.toInt(Foreign.F0);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void wireBehaviorIsUnchanged() {
    // end-to-end: the mapping drives enum encoding/decoding, so a contract-preserving
    // implementation change must leave the bytes and the decoded values alone
    EnumMapping<TrafficLight> mapping = mapping(TrafficLight.class,
        TrafficLight.RED, 0, TrafficLight.YELLOW, 1, TrafficLight.GREEN, 2);
    Struct struct = StructBuilder.newStructBuilder().enm("light", 10, mapping).build();
    ByteBuffer encoded = struct.encoder().enm("light", TrafficLight.GREEN).encode();
    StructDecoder<?> decoder = struct.decoder(encoded.duplicate().position(0));
    assertThat(decoder.enm("light", TrafficLight.RED), is(TrafficLight.GREEN));
    assertThat(impl(mapping), is("DenseOrdinalEnumMapping"));
  }
}