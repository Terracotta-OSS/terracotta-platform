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
package org.terracotta.runnel.encoding;

import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.runnel.EnumMapping;
import org.terracotta.runnel.EnumMappingBuilder;
import org.terracotta.runnel.Struct;
import org.terracotta.runnel.StructBuilder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class EncodingPerfTest {

  enum Typ {
    BOOL,
    CHAR,
    INT,
    LONG,
    DOUBLE,
    STRING,
    BYTES;
  }

  public static final EnumMapping<Typ> TYPE_ENUM_MAPPING = EnumMappingBuilder.newEnumMappingBuilder(Typ.class)
                                                                             .mapping(Typ.BOOL,
                                                                                      0)
                                                                             .mapping(Typ.CHAR, 1)
                                                                             .mapping(Typ.INT, 2)
                                                                             .mapping(Typ.LONG, 3)
                                                                             .mapping(Typ.DOUBLE, 4)
                                                                             .mapping(Typ.STRING, 5)
                                                                             .mapping(Typ.BYTES, 6)
                                                                             .build();

  public static final Struct KEY_STRUCT = StructBuilder.newStructBuilder().enm("keyType", 10, TYPE_ENUM_MAPPING).bool(
    "bool",
    20).chr("char", 30).int32("int", 40).int64("long", 50).fp64("double", 60).string("string", 70).build();

  public static final Struct CELL_STRUCT = StructBuilder.newStructBuilder()
                                                        .string("name", 10)
                                                        .enm("type",
                                                             20,
                                                             TYPE_ENUM_MAPPING)
                                                        .bool("bool", 30)
                                                        .chr("char", 40)
                                                        .int32("int", 50)
                                                        .int64("long", 60)
                                                        .fp64("double", 70)
                                                        .string("string", 80)
                                                        .byteBuffer("bytes", 90)
                                                        .build();

  private static final Struct RDS = StructBuilder.newStructBuilder()
                                                 .int64("msn", 10)
                                                 .struct("key", 20, KEY_STRUCT)
                                                 .structs("cells", 30, CELL_STRUCT)
                                                 .build();

  static StructEncoder<Void> buildRecord(Random r, int seed, int minStrSize, int maxStrSize) {
    return RDS.encoder()
              .int64("msn", seed)
              .struct("key")
                 .string("string", "key" + seed)
              .end()
              .structs("cells")
              .add()
                 .string("name", "Cell1")
                 .enm("type", Typ.STRING)
                 .string("string", stringValue(r, minStrSize, maxStrSize))
              .end()
              .add()
                 .string("name", "Cell2")
                 .enm("type", Typ.INT)
                 .int32("int", seed)
              .end()
              .add()
                 .string("name", "Cell3")
                 .enm("type", Typ.INT)
                 .int32("int", 2 * seed)
              .end()
              .add()
                 .string("name", "Cell4")
                 .enm("type", Typ.STRING)
                 .string("string", stringValue(r, minStrSize/4, maxStrSize/4))
              .end()
           .end();
  }

  private static String stringValue(Random r, int minsz, int maxsz) {
    int sz = minsz + r.nextInt(maxsz - minsz);
    char[] c = new char[sz];
    for (int i = 0; i < c.length; i++) {
      c[i] = (char) ('A' + r.nextInt(26));
    }
    return new String(c);
  }

  @Test
  @Ignore
  public void quickTest() {
    Random r = new Random(0);
    long totalBytes = 0;
    long totalObjects = 0;
    long totalMillis = 0;

    for (int j = 0; j < 200; j++) {
      ArrayList<StructEncoder<Void>> encoders = new ArrayList<>();
      for (int i = 0; i < 5000; i++) {
        StructEncoder<Void> ret = buildRecord(r, i, 2048, 8192);
        encoders.add(ret);
      }
      long st = System.nanoTime();
      for (StructEncoder<Void> enc : encoders) {
        ByteBuffer b = enc.encode();
        b.clear();
        totalBytes += b.array().length;
        totalObjects++;
      }
      long took = System.nanoTime() - st;
      totalMillis = totalMillis + TimeUnit.MILLISECONDS.convert(took, TimeUnit.NANOSECONDS);
      long objectRateSecs = (1000 * totalObjects) / totalMillis;
      long byteRateSecs = (1000 * totalBytes) / totalMillis;
      System.out.println(j + ". " + took + "ns " + objectRateSecs + " objs/sec " + byteRateSecs + " bytes/sec");
      encoders.clear();
    }
    long objectRateSecs = (1000 * totalObjects) / totalMillis;
    long byteRateSecs = (1000 * totalBytes) / totalMillis;
    System.out.println(objectRateSecs + " objs/sec " + byteRateSecs + " bytes/sec");
  }

}
