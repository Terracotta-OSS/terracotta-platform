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
import org.terracotta.runnel.decoding.StructArrayDecoder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructArrayEncoder;
import org.terracotta.runnel.encoding.StructEncoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * {@link StructArrayDecoder#decodeAll} must return the same values as classic {@code next()}
 * iteration, including when elements are only partially read.
 */
public class StructArrayDecodeAllTest {

  private static final Struct CELL = StructBuilder.newStructBuilder()
      .string("name", 10)
      .int64("value", 20)
      .bool("flag", 30)
      .build();

  private static final Struct STRUCT = StructBuilder.newStructBuilder()
      .int64("count", 5)
      .structs("cells", 10, CELL)
      .build();

  private static ByteBuffer encode(int n) {
    StructEncoder<Void> e = STRUCT.encoder().int64("count", n);
    StructArrayEncoder<StructEncoder<Void>> arr = e.structs("cells");
    for (int i = 0; i < n; i++) {
      arr.add().string("name", "cell-" + i).int64("value", 5500000000L + i).bool("flag", (i & 1) == 0).end();
    }
    return arr.end().encode();
  }

  private static List<String> viaIterator(ByteBuffer bb) {
    StructDecoder<?> d = STRUCT.decoder(bb.position(0));
    d.int64("count");
    StructArrayDecoder<?> arr = d.structs("cells");
    List<String> out = new ArrayList<>();
    while (arr.hasNext()) {
      StructDecoder<?> cell = arr.next();
      out.add(cell.string("name") + "|" + cell.int64("value") + "|" + cell.bool("flag"));
      cell.end();
    }
    return out;
  }

  @Test
  public void decodeAllMatchesIterator() {
    for (int n : new int[]{0, 1, 2, 5, 33}) {
      ByteBuffer enc = encode(n);
      List<String> expected = viaIterator(enc.duplicate().position(0));

      StructDecoder<?> d = STRUCT.decoder(enc.duplicate().position(0));
      d.int64("count");
      List<String> actual = d.structs("cells").decodeAll(cell ->
          cell.string("name") + "|" + cell.int64("value", -1) + "|" + cell.bool("flag", false));
      assertThat("n=" + n, actual, is(expected));
    }
  }

  @Test
  public void decodeAllSkipsPartiallyReadElements() {
    ByteBuffer enc = encode(4);
    StructDecoder<?> d = STRUCT.decoder(enc.duplicate().position(0));
    d.int64("count");
    List<String> names = d.structs("cells").decodeAll(cell -> cell.string("name"));
    assertThat(names, is(java.util.Arrays.asList("cell-0", "cell-1", "cell-2", "cell-3")));
  }

  @Test
  public void decodeAllOnEmptyArray() {
    ByteBuffer enc = encode(0);
    StructDecoder<?> d = STRUCT.decoder(enc.duplicate().position(0));
    d.int64("count");
    List<String> names = d.structs("cells").decodeAll(cell -> cell.string("name"));
    assertThat(names.isEmpty(), is(true));
  }

  @Test
  public void iteratorStillWorks() {
    List<String> out = viaIterator(encode(7));
    assertThat(out.size(), is(7));
    assertThat(out.get(5), is("cell-5|5500000005|false")); // (5 & 1) != 0
  }
}
